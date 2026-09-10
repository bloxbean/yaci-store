package com.bloxbean.cardano.yaci.store.analytics.query.service;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsTransactionQueryServiceTest {

    @Test
    void transactionCountRunsOnTheSharedEngineWithOptionalDateRange() {
        AnalyticsQueryExecutor executor = mock(AnalyticsQueryExecutor.class);
        when(executor.queryForList(anyString())).thenReturn(List.of(Map.of("total", 42L)));
        AnalyticsTransactionQueryService service = new AnalyticsTransactionQueryService(executor);

        assertEquals(42L, service.getTransactionCount(null, null));
        assertEquals(42L, service.getTransactionCount(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 7)));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(executor, times(2)).queryForList(sql.capture());
        assertFalse(sql.getAllValues().get(0).contains("WHERE"), sql.getAllValues().get(0));
        assertTrue(sql.getAllValues().get(1).contains("WHERE date >= DATE '2025-06-01' AND date <= DATE '2025-06-07'"),
                sql.getAllValues().get(1));
        for (String statement : sql.getAllValues()) {
            assertTrue(statement.contains("FROM \"transaction\""), statement);
            assertFalse(statement.contains("ducklake_catalog"), statement);
        }
    }

    @Test
    void emptyResultYieldsZero() {
        AnalyticsQueryExecutor executor = mock(AnalyticsQueryExecutor.class);
        when(executor.queryForList(anyString())).thenReturn(List.of());
        assertEquals(0L, new AnalyticsTransactionQueryService(executor).getTransactionCount(null, null));
    }
}
