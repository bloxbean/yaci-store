package com.bloxbean.cardano.yaci.store.analytics.query.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void rejectsReplacementScanTraversalInRegularAndDollarQuotedStrings() {
        assertRejected("SELECT * FROM 'data/../../../../etc/passwd'");
        assertRejected("SELECT * FROM $$data/../../../../etc/passwd$$");
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
                "SELECT $$SET LOAD GLOB -- /* */$$ AS payload"));
        assertDoesNotThrow(() -> SqlValidator.validate(
                "SELECT E'quoted\\' -- not a comment' AS payload"));
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

    private static void assertRejected(String sql) {
        assertThrows(IllegalArgumentException.class, () -> SqlValidator.validate(sql));
    }
}
