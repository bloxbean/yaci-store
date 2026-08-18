package com.bloxbean.cardano.yaci.store.analytics.query.executor;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Row limiting the way SQL front-ends do it: the statement is wrapped in
 * {@code SELECT * FROM (...) LIMIT n+1}; a real (in-memory, streaming) DuckDB instance stands
 * in for the query engine.
 */
class AnalyticsQueryExecutorLimitTest {

    private DuckDBConnection parent;
    private ParquetReadConnectionProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("jdbc_stream_results", "true");
        parent = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:", props);
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
    }

    @Test
    void listQueriesUseTheHardLimit() {
        AnalyticsQueryExecutor executor = executor(100, 10_000);
        assertEquals(250, executor.queryForList("SELECT x FROM t").size());
        assertEquals(200, executor(100, 200).queryForList("SELECT x FROM t").size());
        assertEquals("SELECT * FROM (\nSELECT 1\n) AS q LIMIT 6", AnalyticsQueryExecutor.withRowLimit("SELECT 1", 5));
    }
}
