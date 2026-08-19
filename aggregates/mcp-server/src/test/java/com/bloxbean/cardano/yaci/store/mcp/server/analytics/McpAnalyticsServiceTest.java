package com.bloxbean.cardano.yaci.store.mcp.server.analytics;

import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpAnalyticsServiceTest {

    private AnalyticsQueryExecutor executor;
    private ParquetReadConnectionProvider connectionProvider;
    private McpAnalyticsService service;

    @BeforeEach
    void setUp() {
        executor = mock(AnalyticsQueryExecutor.class);
        connectionProvider = mock(ParquetReadConnectionProvider.class);
        when(executor.getHardRowLimit()).thenReturn(10_000);
        service = new McpAnalyticsService(mock(AnalyticsSchemaService.class), executor, connectionProvider);
    }

    @Test
    void executeSqlDefaultsTo1000RowsAndReportsLimitsAndTruncation() {
        List<Map<String, Object>> rows = List.of(Map.of("epoch", 500, "tx_count", 42));
        when(executor.execute(eq("SELECT epoch, count(*) AS tx_count FROM transaction GROUP BY epoch"), eq(1_000), isNull()))
                .thenReturn(new AnalyticsQueryExecutor.QueryResult(rows, 1_000, true, 30));

        Map<String, Object> result = service.executeSql(
                "  SELECT epoch, count(*) AS tx_count FROM transaction GROUP BY epoch  ", null, null);

        assertEquals(rows, result.get("rows"));
        assertEquals(1, result.get("row_count"));
        assertEquals(1_000, result.get("row_limit"));
        assertEquals(McpAnalyticsService.DEFAULT_MAX_ROWS, 1_000);
        assertEquals(true, result.get("truncated"));
        assertEquals(30, result.get("timeout_seconds"));
        assertTrue(result.containsKey("execution_time_ms"));
        assertEquals("DuckDB (exported analytics data)", result.get("data_source"));
    }

    @Test
    void executeSqlPassesCallerLimitsThroughAndReportsFederation() {
        when(connectionProvider.isLiveDataActive()).thenReturn(true);
        when(executor.execute(eq("SELECT 1"), eq(50), eq(120)))
                .thenReturn(new AnalyticsQueryExecutor.QueryResult(List.of(), 50, false, 120));

        Map<String, Object> result = service.executeSql("SELECT 1", 50, 120);

        verify(executor).execute("SELECT 1", 50, 120);
        assertEquals(50, result.get("row_limit"));
        assertEquals(120, result.get("timeout_seconds"));
        assertEquals(false, result.get("truncated"));
        assertTrue(String.valueOf(result.get("data_source")).contains("live PostgreSQL"));
    }

    @Test
    void executeSqlRejectsNonSelectStatementsBeforeTouchingTheEngine() {
        assertThrows(IllegalArgumentException.class, () -> service.executeSql("DROP TABLE block", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.executeSql("   ", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.executeSql(null, null, null));
        verify(executor, org.mockito.Mockito.never()).execute(any(), any(), any());
        verifyNoInteractions(connectionProvider);
    }
}
