package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class RollbackExporterTest {
    @Test
    void shouldUseLeftJoinForBlock() {
        StorageWriter storageWriter = mock(StorageWriter.class);
        when(storageWriter.getSourceSchema()).thenReturn("mainnet");

        RollbackExporter exporter = new RollbackExporter(
                storageWriter,
                mock(ExportStateService.class),
                mock(EraService.class),
                new AnalyticsStoreProperties(),
                mock(AdaPotJobStorage.class)
        );

        String sql = exporter.buildQuery(
                PartitionValue.ofDate(LocalDate.of(2024, 1, 15)),
                new SlotRange(100, 200)
        );

        assertThat(sql)
                .contains("LEFT JOIN mainnet.block b ON r.current_slot = b.slot")
                .doesNotContain("INNER JOIN");
    }

}