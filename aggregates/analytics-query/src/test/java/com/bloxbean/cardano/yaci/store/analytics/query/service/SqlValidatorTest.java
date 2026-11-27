package com.bloxbean.cardano.yaci.store.analytics.query.service;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    @Test
    void rejectsTableFunctionsInsideParenthesisedJoins() {
        // DuckDB runs FROM (f(...) CROSS JOIN t): the first item of the group is a table reference too
        assertRejected("SELECT * FROM (ducklake_snapshots('lake') CROSS JOIN block)");
        assertRejected("SELECT count(*) FROM (range(2) AS a CROSS JOIN range(3) AS b)");
        assertRejected("SELECT * FROM ((summary(block)) CROSS JOIN block)");
        assertRejected("SELECT * FROM block, (read_text('x') CROSS JOIN block)");
        assertRejected("SELECT * FROM block JOIN (ducklake_list_files('lake', 'block') CROSS JOIN block) ON true");
    }

    @Test
    void allowsParenthesisedJoinsOfTablesAndSubqueries() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM (block b JOIN transaction t ON b.number = t.block_number)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM (SELECT number, count(*) AS n FROM block GROUP BY number) q"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM (unnest([1, 2]) AS u(x) CROSS JOIN block)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block PIVOT (count(*) FOR era IN ('babbage', 'conway'))"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM block, (SELECT max(number) AS m FROM block) x WHERE number = x.m"));
    }

    // --- Unicode whitespace must not hide a call ---

    /** Characters DuckDB 1.5.5's lexer treats as whitespace, plus other Unicode spaces/format characters. */
    private static final int[] UNICODE_SPACES = {
            0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009,
            0x200A, 0x200B, 0x200C, 0x200D, 0x2028, 0x2029, 0x202F, 0x205F, 0x2060, 0x3000, 0xFEFF, 0x00AD,
            0x0085, 0xE0001};

    @Test
    void rejectsUnicodeSpacesBetweenAFunctionNameAndItsParenthesis() {
        for (int cp : UNICODE_SPACES) {
            String sp = Character.toString(cp);
            assertRejected("SELECT * FROM enable_logging" + sp + "()");
            assertRejected("SELECT * FROM query" + sp + "('SELECT * FROM duckdb_settings()')");
            assertRejected("SELECT * FROM block b, some_future_function" + sp + "()");
            assertRejected("SELECT getenv" + sp + "('HOME')");
            assertRejected("SELECT lpad" + sp + "('a', 1000000000, 'b')");
            assertRejected("SELECT printf" + sp + "('%1000000000d', 1)");
            assertRejected("SELECT 1" + sp + "FROM block");
        }
    }

    @Test
    void everyCharacterDuckDbTreatsAsWhitespaceIsRejected() throws Exception {
        // Guard for DuckDB upgrades: scan the ranges that hold Unicode's spaces and format characters.
        List<Integer> duckDbSpaces = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            for (int cp = 0x80; cp <= 0xFFFF; cp++) {
                if ((cp > 0x3100 && cp < 0xFE00) || Character.isSurrogate((char) cp)) {
                    continue;
                }
                String sql = "SELECT abs" + Character.toString(cp) + "(-42)";
                // a failed query closes its statement, so use one per probe
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    if (rs.next() && rs.getInt(1) == 42) {
                        duckDbSpaces.add(cp);
                    }
                } catch (SQLException notAWhitespace) {
                    // part of an identifier: "abs<cp>" is an unknown function
                }
            }
        }
        assertTrue(duckDbSpaces.contains(0xA0) && duckDbSpaces.contains(0xFEFF), "probe broken: " + duckDbSpaces);
        for (int cp : duckDbSpaces) {
            assertRejected("SELECT abs" + Character.toString(cp) + "(-42)");
        }
    }

    @Test
    void stillAcceptsNonAsciiTextThatIsNotWhitespace() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT 'Café – 日本語 ✓' AS label, \"größe\" FROM block"));
    }

    // --- more table positions the default deny must recognise ---

    /**
     * Each shape puts a table function (%s) where DuckDB treats it as a table reference. Checked
     * against DuckDB with repeat_row and against the validator with a function on no name list.
     */
    private static final String[] TABLE_FUNCTION_SHAPES = {
            // set operation in a call-shaped or keyword-introduced parenthesis
            "WITH x(a) AS (VALUES (1) UNION ALL SELECT 1 FROM %s) SELECT * FROM x",
            "SELECT EXISTS (VALUES (1) UNION ALL SELECT 1 FROM %s)",
            "SELECT 1 WHERE EXISTS (VALUES (1) UNION ALL SELECT 1 FROM %s)",
            "SELECT * FROM (VALUES (1) UNION ALL SELECT 1 FROM %s)",
            "SELECT 1 WHERE 1 IN ((SELECT 1) UNION ALL SELECT 1 FROM %s)",
            "SELECT 1 WHERE 1 IN (VALUES (1) INTERSECT SELECT 1 FROM %s)",
            "SELECT ARRAY(VALUES (1) EXCEPT SELECT 1 FROM %s)",
            // a keyword used as a field name after a dot does not end the FROM list
            "SELECT * FROM (SELECT {'limit': 1} AS s) t JOIN block u ON t.s.limit = 1, %s",
            "SELECT * FROM (SELECT {'where': 1} AS s) t JOIN block u ON t.s.where = 1, %s",
            "SELECT * FROM (SELECT {'select': 1} AS s) t JOIN block u ON t.s. select = 1, %s",
            // quoted identifiers are not IS [NOT] DISTINCT FROM
            "SELECT \"not\" \"distinct\" FROM (SELECT 1 AS \"not\") s, %s",
            "SELECT \"is\" \"distinct\" FROM (SELECT 1 AS \"is\") s, %s",
            // DuckDB's prefix alias
            "SELECT * FROM x: %s",
            "SELECT * FROM \"x\" : %s",
            "SELECT * FROM block, x: %s",
            "SELECT * FROM block JOIN x: %s ON true",
            "SELECT * FROM (x: %s CROSS JOIN block)",
            "SELECT * FROM x: (%s CROSS JOIN block)",
            // a number directly followed by FROM
            "SELECT 1FROM %s",
            "SELECT 1e5FROM %s",
            "SELECT 1.5e-3FROM %s",
            "SELECT 1_000FROM %s",
            "SELECT .5FROM %s",
            "SELECT 1.FROM %s",
    };

    @Test
    void rejectsTableFunctionsInEveryShapeDuckDbAccepts() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE block AS SELECT 1 AS number");
            for (String shape : TABLE_FUNCTION_SHAPES) {
                String sql = String.format(Locale.ROOT, shape, "repeat_row(1, num_rows = 2)");
                try (ResultSet rs = st.executeQuery(sql)) {
                    assertTrue(rs.next(), sql);
                }
            }
        }
        for (String shape : TABLE_FUNCTION_SHAPES) {
            assertRejected(String.format(Locale.ROOT, shape, "some_future_function(1)"));
            assertRejected(String.format(Locale.ROOT, shape, "\"some_future_function\"(1)"));
        }
    }

    @Test
    void allowsTheSameSyntaxWithTablesAndScalarCalls() {
        assertDoesNotThrow(() -> SqlValidator.validate(
                "WITH x(a) AS (VALUES (1) UNION ALL SELECT number FROM block) SELECT * FROM x"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT 1 WHERE EXISTS (VALUES (1) UNION ALL SELECT abs(number) FROM block WHERE trim('x' FROM hash) = '')"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT t.s.limit, t.s.offset FROM (SELECT {'limit': 1, 'offset': 2} AS s) t, block b WHERE b.number = abs(1)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT * FROM b: block JOIN t: transaction ON b.number = t.block_number, u: unnest([1])"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT 1e5, 1.5e-3, .5, 1_000, 1.5 FROM block WHERE slot > 1e3 ORDER BY abs(slot)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT CAST(fee AS DECIMAL(10, 2)), CASE WHEN (slot > 1) THEN (1) ELSE (2) END FROM block"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT slot::DECIMAL(20, 0) FROM block WHERE slot BETWEEN (1) AND abs(2)"));
    }

    // --- fail-closed layer: the engine's table functions are rejected by name ---

    @Test
    void registeredTableFunctionsAreRejectedByNameAnywhere() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            st.execute("CREATE MACRO zz_registry_probe(x) AS TABLE SELECT x AS v");
            assertDoesNotThrow(() -> SqlValidator.validate("SELECT zz_registry_probe(1)"));
            SqlValidator.registerTableFunctions(c);
        }
        assertRejected("SELECT zz_registry_probe(1)");
        assertRejected("SELECT 1 FROM block WHERE zz_registry_probe (1) IS NULL");
        assertRejected("SELECT \"zz_registry_probe\"(1)");
        // the allow-list and names that are also scalar/aggregate functions are unaffected
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT * FROM unnest([1, 2]) AS u(x)"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT j.key FROM transaction_metadata t, json_each(t.body) j"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT histogram(epoch) FROM block"));
    }

    // --- bitstring ---

    @Test
    void rejectsUnboundedBitstrings() {
        assertRejected("SELECT bitstring('1', 1000000000)");
        assertRejected("SELECT length(bitstring('1', 10001))");
        assertRejected("SELECT bitstring('1', slot) FROM block");
        assertRejected("SELECT bitstring(bitstring('1', 8), 2000000000)");
        assertRejected("SELECT hash.bitstring(1000000000) FROM block");
        assertRejected("SELECT main.bitstring('1', 1000000000)");
        assertRejected("SELECT bitstring_agg(slot, 0, 100000000) FROM block GROUP BY epoch");
        assertRejected("SELECT bitstring_agg(slot) FROM block");
    }

    @Test
    void allowsSmallLiteralBitstrings() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT bitstring('101', 8), bitstring('1', 10000)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT main.bitstring('1', 16), hash.bitstring(64) FROM block"));
    }

    // --- schema-qualified calls are not member calls ---

    @Test
    void acceptsSchemaQualifiedFormatPrintfAndPad() {
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT main.format('{}', 42)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT main.printf('%d', 1)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT \"main\" . format('{} {}', 1, 2)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT \"system\".printf('%d', 1)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT \"system\".main.format('{}', 1)"));
        assertDoesNotThrow(() -> SqlValidator.validate("SELECT \"system\".\"main\".lpad('a', 3, 'b')"));
    }

    @Test
    void stillChecksQualifiedCallsAndRealMemberCalls() {
        assertRejected("SELECT main.format('{:>1000000000}', 'a')");
        assertRejected("SELECT main.printf('%1000000000d', 1)");
        assertRejected("SELECT \"system\".main.printf('%*d', 100000000, 1)");
        assertRejected("SELECT main.lpad('a', 1000000000, 'b')");
        // t.main is a column: member calls whose format string is the column value
        assertRejected("SELECT t.main.format('{}', 1) FROM (SELECT '{:>100000000}' AS main) t");
        assertRejected("SELECT main.main.format('{}', 1) FROM (SELECT '{:>100000000}' AS main) main");
        assertRejected("SELECT \"memory\".main.format('{}', 1) FROM (SELECT '{:>100000000}' AS main) \"memory\"");
        assertRejected("SELECT \"temp\".main.printf('%d', 1) FROM (SELECT '%100000000d' AS main) \"temp\"");
        assertRejected("SELECT x.\"system\".main.format('{}', 1) FROM block x");
    }

    @Test
    void duckDbResolvesSchemaQualifiersBeforeColumns() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = c.createStatement()) {
            String[][] cases = {
                    // qualified: the literal is the format string even though a column "main" exists
                    {"SELECT main.format('{}|', 'a') FROM (SELECT '{}-{}' AS main)", "a|"},
                    {"SELECT \"system\".main.format('{}|', 'a') FROM (SELECT '{}-{}' AS main) \"system\"", "a|"},
                    // member calls: the column is the format string
                    {"SELECT t.main.format('{}|', 'a') FROM (SELECT '{}-{}' AS main) t", "{}|-a"},
                    {"SELECT \"memory\".main.format('{}|', 'a') FROM (SELECT '{}-{}' AS main) \"memory\"", "{}|-a"},
            };
            for (String[] tc : cases) {
                try (ResultSet rs = st.executeQuery(tc[0])) {
                    assertTrue(rs.next(), tc[0]);
                    assertEquals(tc[1], rs.getString(1), tc[0]);
                }
            }
        }
    }

    private static void assertRejected(String sql) {
        assertThrows(IllegalArgumentException.class, () -> SqlValidator.validate(sql), sql);
    }
}
