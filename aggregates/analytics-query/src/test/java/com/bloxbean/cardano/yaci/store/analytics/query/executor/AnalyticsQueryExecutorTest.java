package com.bloxbean.cardano.yaci.store.analytics.query.executor;

import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Array;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsQueryExecutorTest {

    @Test
    void normalizesDuckDbArraysAndBlobsForJsonSerialization() throws Exception {
        Array sqlArray = mock(Array.class);
        when(sqlArray.getArray()).thenReturn(new Object[]{"asset", 42L});

        assertEquals(List.of("asset", 42L), AnalyticsQueryExecutor.normalizeJdbcValue(sqlArray));
        verify(sqlArray).free();

        assertEquals("AQID", AnalyticsQueryExecutor.normalizeJdbcValue(
                new SerialBlob(new byte[]{1, 2, 3})));
        assertEquals("AQID", AnalyticsQueryExecutor.normalizeJdbcValue(new byte[]{1, 2, 3}));
    }
}
