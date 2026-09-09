package com.bloxbean.cardano.yaci.store.analytics.query.executor;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Row limiting the way SQL front-ends do it: the statement is wrapped in
 * {@code SELECT * FROM (...) LIMIT n+1}; a real in-memory DuckDB instance (opened exactly like
 * {@code DuckDbReadConnectionProvider} opens it, i.e. without result streaming) stands in for
 * the query engine.
 */
class AnalyticsQueryExecutorLimitTest {

    private DuckDBConnection parent;
    private ParquetReadConnectionProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        parent = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        try (Statement stmt = parent.createStatement()) {
            stmt.execute("CREATE TABLE t AS SELECT range AS x, 'v' || range AS v FROM range(250)");
        }
        provider = mock(ParquetReadConnectionProvider.class);
        when(provider.getReadConnection()).thenAnswer(inv -> parent.duplicate());
        when(provider.getQueryTimeoutSeconds()).thenReturn(30);
    }

    @AfterEach
    void tearDown() throws SQLException {
        parent.close();
    }

    private AnalyticsQueryExecutor executor(int defaultMaxRows, int maxRows) {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getQuery().setDefaultMaxRows(defaultMaxRows);
        properties.getQuery().setMaxRows(maxRows);
        return new AnalyticsQueryExecutor(provider, properties);
    }

    @Test
    void defaultLimitAppliesWhenCallerGivesNone() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        AnalyticsQueryExecutor.QueryResult result = executor.execute("SELECT x FROM t", null);
        assertEquals(100, result.rows().size());
        assertEquals(100, result.rowLimit());
        assertTrue(result.truncated());
    }

    @Test
    void callerLimitIsHonouredAndCappedByTheHardLimit() {
        AnalyticsQueryExecutor executor = executor(100, 200);
        AnalyticsQueryExecutor.QueryResult small = executor.execute("SELECT x FROM t", 50);
        assertEquals(50, small.rows().size());
        assertTrue(small.truncated());

        AnalyticsQueryExecutor.QueryResult capped = executor.execute("SELECT x FROM t", 5_000);
        assertEquals(200, capped.rowLimit());
        assertEquals(200, capped.rows().size());
        assertTrue(capped.truncated());

        AnalyticsQueryExecutor.QueryResult all = executor(100, 10_000).execute("SELECT x FROM t", 1_000);
        assertEquals(250, all.rows().size());
        assertFalse(all.truncated());
    }

    @Test
    void configuredLimitsAreClampedToTheBuiltInCeiling() {
        AnalyticsQueryExecutor executor = executor(50_000, 50_000);
        assertEquals(AnalyticsQueryExecutor.MAX_RESULT_ROWS, executor.getHardRowLimit());
        assertEquals(AnalyticsQueryExecutor.MAX_RESULT_ROWS, executor.getDefaultRowLimit());

        AnalyticsQueryExecutor unset = executor(0, 0);
        assertEquals(AnalyticsQueryExecutor.MAX_RESULT_ROWS, unset.getHardRowLimit());
        assertEquals(AnalyticsQueryExecutor.MAX_RESULT_ROWS, unset.getDefaultRowLimit());

        // default-max-rows can never exceed max-rows
        assertEquals(20, executor(500, 20).getDefaultRowLimit());
    }

    @Test
    void wrapperPreservesInnerOrderByAndInnerLimit() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        List<Map<String, Object>> top = executor.execute("SELECT x FROM t ORDER BY x DESC", 3).rows();
        assertEquals(List.of(249L, 248L, 247L), top.stream().map(r -> r.get("x")).toList());

        AnalyticsQueryExecutor.QueryResult inner = executor.execute("SELECT x FROM t ORDER BY x LIMIT 10", 100);
        assertEquals(10, inner.rows().size());
        assertFalse(inner.truncated());
    }

    @Test
    void wrapperSurvivesTrailingCommentsCtesAndDuplicateColumnNames() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        assertEquals(250, executor.execute("SELECT x FROM t -- trailing comment", 1_000).rows().size());
        assertEquals(3, executor.execute(
                "WITH top AS (SELECT x FROM t ORDER BY x DESC LIMIT 3) SELECT * FROM top", 100).rows().size());
        Map<String, Object> row = executor.execute("SELECT 1 AS a, 2 AS a, count(*), count(*) FROM t", 5).rows().get(0);
        assertEquals(4, row.size(), row.toString());   // DuckDB de-duplicates the labels (a, a_1, ...)
        assertEquals(1, row.get("a"));
        assertEquals(2, row.get("a_1"));
    }

    @Test
    void listQueriesUseTheHardLimit() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        assertEquals(250, executor.queryForList("SELECT x FROM t").size());
        assertEquals(200, executor(100, 200).queryForList("SELECT x FROM t").size());
        assertEquals("SELECT * FROM (\nSELECT 1\n) AS q LIMIT 6", AnalyticsQueryExecutor.withRowLimit("SELECT 1", 5));
    }

    /**
     * Regression test for the query timeout. With DuckDB JDBC result streaming
     * ({@code jdbc_stream_results=true}) the cancel timer only covers the execute phase: as soon
     * as the first ~1 MB of rows ({@code streaming_buffer_size}) is buffered, {@code executeQuery}
     * returns and the rest of the query runs unbounded inside {@code ResultSet.next()}. The
     * engine therefore uses non-streaming result sets. This shape — 10,000 wide cheap rows
     * (~1.7 MB, above the streaming buffer) followed by a filter scan over 4e9 values that
     * never matches ({@code hash()} defeats statistics-based pruning) — must be cancelled by
     * the 1-second timeout; with streaming it would run for tens of seconds and complete.
     */
    @Test
    void queryTimeoutCoversTheWholeExecutionNotJustTheExecutePhase() {
        when(provider.getQueryTimeoutSeconds()).thenReturn(1);
        AnalyticsQueryExecutor executor = executor(100, 10_000);

        StringBuilder wideColumns = new StringBuilder("range AS x");
        for (int i = 1; i <= 20; i++) {
            wideColumns.append(", range AS c").append(i);
        }
        String cheapRowsThenLongScan = "SELECT " + wideColumns + " FROM range(4000000000) "
                + "WHERE range < 10000 OR hash(range + 1) = 0";

        long start = System.currentTimeMillis();
        assertThrows(AnalyticsQueryExecutor.QueryTimeoutException.class,
                () -> executor.execute(cheapRowsThenLongScan, 10_000));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 500, "query failed too quickly to have exercised the timeout: " + elapsed + "ms");
        assertTrue(elapsed < 5_000, "query was not cancelled by the timeout; took " + elapsed + "ms");
    }

    /**
     * A caller may ask for its own timeout (MCP tools): it is applied per statement, capped by
     * {@code query.max-timeout-seconds}, and never lower than the default when the maximum is
     * misconfigured below it.
     */
    @Test
    void perCallTimeoutIsAppliedAndCappedByTheMaximum() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getQuery().setMaxTimeoutSeconds(120);
        AnalyticsQueryExecutor executor = new AnalyticsQueryExecutor(provider, properties);   // default timeout 30 s
        assertEquals(30, executor.getDefaultTimeoutSeconds());
        assertEquals(120, executor.getMaxTimeoutSeconds());

        assertEquals(30, executor.execute("SELECT 1", null, null).timeoutSeconds());
        assertEquals(30, executor.execute("SELECT 1", null, 0).timeoutSeconds());
        assertEquals(90, executor.execute("SELECT 1", null, 90).timeoutSeconds());
        assertEquals(120, executor.execute("SELECT 1", null, 3_600).timeoutSeconds());

        // A per-call timeout of 1 s cancels the endless scan even though the default is 30 s
        StringBuilder wideColumns = new StringBuilder("range AS x");
        for (int i = 1; i <= 20; i++) {
            wideColumns.append(", range AS c").append(i);
        }
        String endless = "SELECT " + wideColumns + " FROM range(4000000000) WHERE range < 10000 OR hash(range + 1) = 0";
        long start = System.currentTimeMillis();
        assertThrows(AnalyticsQueryExecutor.QueryExecutionException.class,
                () -> executor.execute(endless, 10_000, 1));
        assertTrue(System.currentTimeMillis() - start < 5_000);

        // max-timeout below the default: the default still applies
        properties.getQuery().setMaxTimeoutSeconds(5);
        assertEquals(30, new AnalyticsQueryExecutor(provider, properties).getMaxTimeoutSeconds());
    }

    @Test
    void temporalValuesAreRenderedAsIsoStrings() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        Map<String, Object> row = executor.execute(
                "SELECT DATE '2026-08-19' AS d, TIMESTAMP '2026-08-19 10:00:00.123456' AS ts, "
                        + "TIMESTAMPTZ '2026-08-19 10:00:00+02' AS tstz, TIME '10:00:00' AS t, "
                        + "to_timestamp(1787098225) AS epoch_ts", 1).rows().get(0);
        assertEquals("2026-08-19", row.get("d"));
        assertEquals("2026-08-19T10:00:00.123456", row.get("ts"));
        assertEquals("2026-08-19T08:00:00Z", row.get("tstz"));        // normalized to UTC
        assertEquals("10:00:00", row.get("t"));
        assertEquals("2026-08-19T00:10:25Z", row.get("epoch_ts"));
    }

    @Test
    void binderErrorContainingTimeoutIsNotMisclassifiedAsExecutionTimeout() {
        AnalyticsQueryExecutor.QueryExecutionException error = assertThrows(
                AnalyticsQueryExecutor.QueryExecutionException.class,
                () -> executor(100, 10_000).execute("SELECT timeout FROM t", 10));

        assertEquals(AnalyticsQueryExecutor.QueryExecutionException.class, error.getClass());
    }

    @Test
    void logPreviewIsSingleLineAndBounded() {
        assertEquals("SELECT x FROM t WHERE v = 'a'",
                AnalyticsQueryExecutor.preview("SELECT x\n  FROM t\r\n WHERE   v = 'a'  "));
        String longSql = "SELECT " + "x, ".repeat(100) + "y FROM t";
        String preview = AnalyticsQueryExecutor.preview(longSql);
        assertEquals(103, preview.length());
        assertTrue(preview.endsWith("..."));
    }
}
