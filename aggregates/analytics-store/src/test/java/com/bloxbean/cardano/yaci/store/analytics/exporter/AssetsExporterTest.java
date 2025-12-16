package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetsExporterTest {

    @Test
    void shouldBuildQuery_withSchemaAndSlotRange() {
        StorageWriter storageWriter = mock(StorageWriter.class);
        when(storageWriter.getSourceSchema()).thenReturn("mainnet");

        ExportStateService stateService = mock(ExportStateService.class);
        EraService eraService = mock(EraService.class);
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();

        AssetsExporter exporter = new AssetsExporter(storageWriter, stateService, eraService, properties);

        String sql = exporter.buildQuery(
                PartitionValue.ofDate(LocalDate.of(2024, 1, 15)),
                new SlotRange(10, 20)
        );

        assertThat(sql).contains("FROM source_db.mainnet.assets a");
        assertThat(sql).contains("WHERE a.slot >= 10");
        assertThat(sql).contains("AND a.slot < 20");
        assertThat(sql).contains("ORDER BY a.slot, a.tx_hash, a.unit");
    }
}

