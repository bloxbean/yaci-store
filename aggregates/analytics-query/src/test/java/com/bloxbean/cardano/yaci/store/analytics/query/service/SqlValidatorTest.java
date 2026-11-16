package com.bloxbean.cardano.yaci.store.analytics.query.service;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlValidatorTest {

    @Test
    void rejectsFileReadingFunctionsAndAliases() {
        assertRejected("SELECT * FROM sniff_csv('data/../../../../etc/passwd')");
        assertRejected("SELECT * FROM parquet_scan('data/../../../../etc/passwd')");
        assertRejected("SELECT * FROM read_csv_auto('data.csv')");
        assertRejected("SELECT * FROM \"sniff_csv\"($$data.csv$$)");
        assertRejected("SELECT * FROM postgres_scan('host=example', 'public', 'block')");
    }

    @Test
    void rejectsReplacementScansForAbsoluteTraversalAndOrdinaryRelativePaths() {
        assertRejected("SELECT * FROM 'data/../../../../etc/passwd'");
        assertRejected("SELECT * FROM $$data/../../../../etc/passwd$$");
        assertRejected("SELECT * FROM 'data/analytics/some-file.parquet'");
        assertRejected("SELECT * FROM 'block/date=2026-01-01/file.parquet'");
        assertRejected("SELECT * FROM 'some.csv'");
        assertRejected("SELECT * FROM $$some.csv$$");
        assertRejected("SELECT * FROM E'some.csv'");
    }

    @Test
    void rejectsQuotedBareAndJoinedReplacementScans() {
        assertRejected("SELECT * FROM \"data.csv\"");
        assertRejected("SELECT * FROM \"../data.csv\"");
        assertRejected("SELECT * FROM \"/abs/export/dir/block/x.parquet\"");
        assertRejected("SELECT * FROM data.csv");
        assertRejected("SELECT * FROM block.parquet");
        assertRejected("SELECT * FROM block b JOIN data.csv d ON true");
        assertRejected("SELECT * FROM \"data\".\"csv\"");
        assertRejected("SELECT * FROM data.\"csv\"");
        assertRejected("SELECT * FROM events.jsonl.gz");
    }

    @Test
    void deliberatelyRejectsFileShapedValuesToKeepTheRuleAuditable() {
        assertRejected("SELECT 'some.csv' AS label FROM block");
        assertRejected("SELECT 'https://example.com/value' AS label FROM block");
    }

    @Test
    void allowsOrdinaryFromExpressionsAndValues() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT extract(year FROM '2024-01-01'::DATE)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT trim('x' FROM owner_addr) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block WHERE hash IS DISTINCT FROM 'literal'"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT pivot AS label, 'literal' FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM (FROM block SELECT number, 'lovelace' AS u)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "WITH p AS (PIVOT block ON type IN ('ebb', 'babbage') USING count(*)) SELECT * FROM p"));
    }

    @Test
    void acceptsCardanoIdentifiersThatContainBlockedWordFragments() {
        assertDoesNotThrow(() -> SqlValidator.validate("""
                SELECT asset, payload, upload, global_total
                FROM multi_asset
                WHERE asset = 'lovelace'
                LIMIT 10 OFFSET 5
                """));
    }

    @Test
    void ignoresBlockedWordsAndCommentMarkersInsideStringLiterals() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT 'SET LOAD GLOB -- /* */' AS payload"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT E'quoted\\' -- not a comment' AS payload"));
    }

    @Test
    void rejectsAllDollarSyntaxInsteadOfReimplementingDuckDbTagRules() {
        assertRejected("SELECT $$value$$");
        assertRejected("SELECT $tag$value$tag$");
        assertRejected("SELECT $€$'$€$ FROM read_csv('evil.csv')");
        assertRejected("SELECT $1$value$1$");
        assertRejected("SELECT 1 -- $ in a comment");
    }

    @Test
    void rejectsSerializedSqlMetadataAndHeapBurstFunctions() {
        assertRejected("SELECT * FROM json_execute_serialized_sql(json_serialize_sql('SELECT * FROM duckdb_databases()'))");
        assertRejected("SELECT * FROM parquet_metadata('data.parquet')");
        assertRejected("SELECT * FROM parquet_schema('data.parquet')");
        assertRejected("SELECT * FROM parquet_file_metadata(chr(47) || 'tmp/data.parquet')");
        assertRejected("SELECT repeat('x', 100000000)");
    }

    @Test
    void stillRejectsDangerousTokensAndMultipleStatements() {
        assertRejected("WITH deleted AS (DELETE FROM block RETURNING *) SELECT * FROM deleted");
        assertRejected("SELECT * FROM duckdb_settings()");
        assertRejected("SELECT 1; SELECT 2");
        assertRejected("SELECT 1 AS \"x--\"; DROP VIEW secret");
        assertRejected("SELECT E'quoted\\' -- text'; DROP VIEW secret");
        assertRejected("SELECT 'literal;semicolon'");
        assertRejected("SELECT * FROM query('SELECT path FROM duckdb_databases()')");
        assertRejected("SELECT * FROM query_table('sqlite_master')");
        assertRejected("SELECT * FROM sqlite_master");
        assertRejected("OFFSET 5");
    }

    @Test
    void rejectsMetadataStatementsAndPragmaFunctionsInSubqueryPosition() {
        assertRejected("SELECT * FROM (SHOW ALL TABLES)");
        assertRejected("SELECT * FROM (SHOW TABLES)");
        assertRejected("SELECT * FROM (SHOW DATABASES)");
        assertRejected("SELECT * FROM (show block)");
        assertRejected("SELECT * FROM (DESCRIBE block)");
        assertRejected("SELECT * FROM (DESC block)");
        assertRejected("SELECT * FROM ( desc block) AS d");
        assertRejected("WITH d AS (/* c */ DESC block) SELECT * FROM d");
        assertRejected("SELECT * FROM (SUMMARIZE block)");
        assertRejected("SELECT * FROM pragma_table_info('block')");
        assertRejected("SELECT * FROM pragma_database_size()");
        assertRejected("SELECT * FROM \"pragma_table_info\"('block')");
        assertRejected("SELECT * FROM (FROM duckdb_tables())");
        assertRejected("SELECT * FROM (SELECT * FROM (SHOW ALL TABLES)) LIMIT 1");
        assertRejected("SELECT * FROM pg_live.public.block");
    }

    @Test
    void allowsOrderByDescAndRecursiveCtes() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT slot FROM block ORDER BY slot DESC LIMIT 10"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT slot FROM block ORDER BY slot DESC, epoch DESC NULLS LAST"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT row_number() OVER (ORDER BY slot DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT array_agg(slot ORDER BY slot DESC) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM (SELECT slot FROM block ORDER BY slot DESC) t"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "WITH RECURSIVE t(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM t WHERE n < 5) SELECT * FROM t"));
    }

    @Test
    void allowsQuotedIdentifiersThatMerelySpellKeywords() {
        // Custom-exporter tables may carry arbitrary column names; a double-quoted name is an
        // identifier and can never start a SET/SHOW/LOAD statement.
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT \"set\", \"show\", \"load\", \"system\", \"getenv\", \"desc\", \"call\" FROM custom_table"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT t.\"update\" AS \"delete\" FROM \"insert\" t WHERE t.\"copy\" = 1 ORDER BY (\"desc\") DESC"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM \"block\" WHERE \"summarize\" IS NOT NULL"));
    }

    @Test
    void stillRejectsQuotedFunctionAndCatalogNames() {
        assertRejected("SELECT * FROM \"read_csv\"('data.csv')");
        assertRejected("SELECT * FROM \"sniff_csv\" ('data.csv')");
        assertRejected("SELECT \"getenv\"('HOME')");
        assertRejected("SELECT * FROM \"pg_live\".\"public\".\"block\"");
        assertRejected("SELECT * FROM \"pragma_table_info\"('block')");
        assertRejected("SELECT * FROM \"duckdb_databases\"()");
        assertRejected("SELECT * FROM \"information_schema\".\"tables\"");
        assertRejected("SELECT * FROM \"sqlite_master\"");
        assertRejected("SELECT * FROM \"query\"('SELECT 1')");
    }

    // --- line comments end at '\r' as well as '\n' (DuckDB's lexer) ---

    @Test
    void lineCommentEndsAtBareCarriageReturnLikeDuckDb() {
        // DuckDB runs ", * FROM postgres_query(...)" after the bare CR; the validator must see it too.
        assertRejected("SELECT 1 AS x -- c\r, * FROM postgres_query('pg_live', 'SELECT 1')");
        assertRejected("SELECT 1 AS x -- c\r, getenv('HOME') AS h");
        assertRejected("SELECT 1 AS x -- c\r FROM read_parquet('x')");
        assertEquals("SELECT 1  \r, 2", SqlValidator.stripComments("SELECT 1 -- c\r, 2"));
    }

    @Test
    void duckDbLexerBehaviourTheValidatorReliesOn() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            // A bare CR ends a line comment: the second column is executed.
            try (ResultSet rs = st.executeQuery("SELECT 1 AS x -- c\r, 42 AS smuggled")) {
                assertEquals(2, rs.getMetaData().getColumnCount());
            }
            // Other "newline" characters do not end it.
            for (String sep : new String[]{"\u0085", "\u2028", "\u2029"}) {
                try (ResultSet rs = st.executeQuery("SELECT 1 AS x -- c" + sep + ", 42 AS smuggled\n")) {
                    assertEquals(1, rs.getMetaData().getColumnCount());
                }
            }
            // NUL ends the statement: the text after it is never seen.
            try (ResultSet rs = st.executeQuery("SELECT 1 AS x\0, 42 AS dropped")) {
                assertEquals(1, rs.getMetaData().getColumnCount());
            }
        }
    }

    @Test
    void stillAcceptsCommentsWithWindowsAndUnixLineEndings() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT slot -- the slot\r\nFROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT slot -- the slot\nFROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT slot\r\nFROM block\r\nWHERE epoch = 1\t-- tab"));
    }

    @Test
    void rejectsNulAndOtherControlCharacters() {
        // DuckDB stops at NUL, which would cut off the executor's LIMIT wrapper.
        assertRejected("SELECT * FROM block) AS q\0");
        assertRejected("SELECT 1 \u0000, 2");
        assertRejected("SELECT 1 \u000b, 2");
        assertRejected("SELECT 1 \f, 2");
        assertRejected("SELECT 1 \u007f, 2");
    }

    // --- table functions: default deny in table position ---

    @Test
    void rejectsStateChangingTableFunctions() {
        assertRejected("SELECT * FROM enable_logging(storage = 'file', storage_path = 'logs')");
        assertRejected("SELECT * FROM enable_logging()");
        assertRejected("SELECT * FROM disable_logging()");
        assertRejected("SELECT * FROM truncate_duckdb_logs()");
        assertRejected("SELECT * FROM enable_profiling()");
        assertRejected("SELECT * FROM disable_profiling()");
        assertRejected("SELECT * FROM enable_peg_parser()");
        assertRejected("SELECT * FROM disable_peg_parser()");
        assertRejected("SELECT * FROM force_checkpoint()");
        assertRejected("SELECT * FROM checkpoint()");
        assertRejected("SELECT write_log('x')");
        assertRejected("SELECT sleep_ms(100000)");
        assertRejected("SELECT * FROM repeat_row(1, num_rows = 1000000000)");
        assertRejected("SELECT * FROM histogram('pg_live.public.block', slot)");
    }

    @Test
    void rejectsUnknownTableFunctionsInEveryTablePosition() {
        // Not in any name list: rejected only because it is called in table position.
        assertRejected("SELECT * FROM some_future_function()");
        assertRejected("SELECT * FROM block b JOIN some_future_function() f ON true");
        assertRejected("SELECT * FROM block b LEFT OUTER JOIN some_future_function() f ON true");
        assertRejected("SELECT * FROM block b, some_future_function() f");
        assertRejected("SELECT * FROM block b JOIN epoch e ON b.epoch = e.number, some_future_function()");
        assertRejected("SELECT * FROM block b, LATERAL some_future_function(b.slot)");
        assertRejected("SELECT * FROM block b POSITIONAL JOIN some_future_function()");
        assertRejected("SELECT * FROM (FROM some_future_function())");
        assertRejected("SELECT 1 WHERE EXISTS (FROM some_future_function())");
        assertRejected("SELECT 1 WHERE 1 IN (SELECT * FROM some_future_function())");
        assertRejected("SELECT * FROM main.some_future_function()");
        assertRejected("SELECT * FROM \"main\".\"some_future_function\"()");
        assertRejected("SELECT * FROM (PIVOT some_future_function() ON x USING count(*))");
        assertRejected("WITH t AS (SELECT * FROM some_future_function()) SELECT * FROM t");
    }

    @Test
    void allowsUnnestAndJsonTableFunctionsAndScalarCallsNearFrom() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT * FROM unnest([1, 2, 3]) AS u(x)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT t.tx_hash, j.key FROM transaction_metadata t, json_each(t.body) j"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM transaction_metadata t CROSS JOIN LATERAL json_tree(t.body)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT extract(year FROM to_timestamp(block_time)) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT trim(both 'x' FROM lower(owner_addr)), substring(hash FROM 1 FOR 8) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block WHERE hash IS DISTINCT FROM lower('ab') AND slot IS NOT DISTINCT FROM abs(1)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block b JOIN epoch e ON e.number = abs(b.epoch), (SELECT 1 AS one) o"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block b, LATERAL (SELECT count(*) FROM transaction t WHERE t.slot = b.slot) c"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT epoch, sum(fee) FROM block GROUP BY epoch ORDER BY lower(epoch::VARCHAR), abs(epoch)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT histogram(epoch), count(*) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block AS b(x, y), unnest([1]) WHERE x > coalesce(y, 0)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block USING SAMPLE reservoir(10 ROWS)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT 1.5e3, list_transform([1, 2], x -> x + 1) FROM block"));
    }

    @Test
    void rejectsEveryDuckDbTableFunctionExceptTheAllowList() throws Exception {
        // Guard for DuckDB upgrades: every table function / table macro the engine ships is
        // rejected in table position, and the table-only ones are also rejected by name.
        List<String[]> functions = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT function_name,
                            bool_and(function_type IN ('table', 'table_macro', 'pragma')) AS table_only
                     FROM duckdb_functions()
                     GROUP BY function_name
                     HAVING bool_or(function_type IN ('table', 'table_macro'))
                     """)) {
            while (rs.next()) {
                functions.add(new String[]{rs.getString(1), String.valueOf(rs.getBoolean(2))});
            }
        }
        assertTrue(functions.size() > 50, "expected DuckDB's table functions, got " + functions.size());
        for (String[] f : functions) {
            String name = f[0];
            if (SqlValidator.ALLOWED_TABLE_FUNCTIONS.contains(name.toUpperCase(Locale.ROOT))) {
                continue;
            }
            assertRejected("SELECT * FROM " + name + "()");
            assertRejected("SELECT * FROM block b JOIN \"" + name + "\"(1) x ON true");
            if (Boolean.parseBoolean(f[1])) {
                assertRejected("SELECT " + name + "(1)");
            }
        }
    }

    // --- allocations that memory_limit does not cover ---

    @Test
    void rejectsUnboundedPadAndResize() {
        assertRejected("SELECT length(lpad('a', 1000000000, 'b'))");
        assertRejected("SELECT rpad('a', 10001, 'b')");
        assertRejected("SELECT lpad('a', slot, 'b') FROM block");
        assertRejected("SELECT lpad('a', 100 * 100000, 'b')");
        assertRejected("SELECT lpad('a', 1e9::INT, 'b')");
        assertRejected("SELECT owner_addr.lpad(1000000000, 'b') FROM block");
        assertRejected("SELECT \"lpad\"('a', 1000000000, 'b')");
        assertRejected("SELECT len(list_resize([1::BIGINT], 300000000))");
        assertRejected("SELECT len(array_resize([1::BIGINT], 3))");
        assertRejected("SELECT [1].list_resize(3)");
    }

    @Test
    void allowsSmallLiteralPadWidths() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT lpad(epoch::VARCHAR, 4, '0') FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT rpad(hash, 10000, ' ') FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT main.lpad(hash, 8, '0') FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT hash.lpad(70, '0') FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT lpad(rpad(hash, 70, 'x'), 80, '0') FROM block"));
    }

    @Test
    void rejectsWideOrDynamicFormatSpecs() {
        assertRejected("SELECT printf('%1000000000d', 1)");
        assertRejected("SELECT printf('%.100000000f', 1.0)");
        assertRejected("SELECT printf('%*d', 100000000, 1)");
        assertRejected("SELECT printf('%.*f', 100000000, 1.0)");
        assertRejected("SELECT format('{:>1000000000}', 'a')");
        assertRejected("SELECT format('{:>{}}', 'a', 100000000)");
        assertRejected("SELECT format('{0:.{1}f}', 1.0, 100000000)");
        assertRejected("SELECT format(hash, 1) FROM block");
        assertRejected("SELECT ('{:>100000000}').format('a')");
        assertRejected("SELECT format(E'{:>100000000}', 'a')");
    }

    @Test
    void allowsOrdinaryFormatting() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT printf('%5.2f ADA (%d%%) %-10s', fee / 1e6, 5, hash) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT format('{:,} lovelace in epoch {} of 100000 {{literal}}', fee, epoch) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT format('{:*>12}|{:.3f}|{0}', hash, 1.5) FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT format('it''s {}', 1)"));
    }

    private static void assertRejected(String sql) {
        assertThrows(IllegalArgumentException.class, () -> SqlValidator.validate(sql), sql);
    }
}
