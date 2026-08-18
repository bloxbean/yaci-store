package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The unified-view metadata exporters expose to the analytics query layer
 * ({@link TableExporter#getFederationBoundaryColumn()}, {@link TableExporter#getSourceColumnMappings()}).
 */
class TableExporterFederationMetadataTest {

    @Test
    void boundaryColumnDefaultsFollowThePartitionStrategy() {
        assertThat(exporterWith(PartitionStrategy.DAILY).getFederationBoundaryColumn()).isEqualTo("slot");
        assertThat(exporterWith(PartitionStrategy.EPOCH).getFederationBoundaryColumn()).isEqualTo("epoch");
        assertThat(exporterWith(PartitionStrategy.MONTHLY).getFederationBoundaryColumn()).isNull();
        assertThat(exporterWith(PartitionStrategy.DAILY).getSourceColumnMappings()).isEmpty();
    }

    @Test
    void spentOutputsAreBoundedBySpendSlot() {
        SpentOutputsExporter exporter = new SpentOutputsExporter(writer(), stateService(), eraService(), props(), adaPot());
        assertThat(exporter.getPartitionStrategy()).isEqualTo(PartitionStrategy.DAILY);
        assertThat(exporter.getFederationBoundaryColumn()).isEqualTo("spent_at_slot");
        // and the export query really produces that column
        assertThat(exporter.buildQuery(PartitionValue.ofDate(LocalDate.of(2024, 1, 15)), new SlotRange(10, 20)))
                .contains("spent_at_slot");
    }

    @Test
    void epochKeyedTablesDeclareHowTheirExportedEpochMapsToPostgres() {
        RewardExporter reward = new RewardExporter(writer(), stateService(), eraService(), props(), adaPot());
        assertThat(reward.getFederationBoundaryColumn()).isEqualTo("epoch");
        assertThat(reward.getSourceColumnMappings()).isEqualTo(Map.of("epoch", "earned_epoch"));
        assertThat(reward.buildQuery(PartitionValue.ofEpoch(300), new SlotRange(10, 20)))
                .contains("earned_epoch AS epoch");

        ConstitutionExporter constitution = new ConstitutionExporter(writer(), stateService(), eraService(), props(), adaPot());
        assertThat(constitution.getSourceColumnMappings()).isEqualTo(Map.of("epoch", "active_epoch"));

        EpochExporter epoch = new EpochExporter(writer(), stateService(), eraService(), props(), adaPot(),
                mock(com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader.class));
        assertThat(epoch.getSourceColumnMappings()).isEqualTo(Map.of("epoch", "number"));

        PoolRegistrationExporter pool = new PoolRegistrationExporter(writer(), stateService(), eraService(), props(), adaPot());
        assertThat(pool.getFederationBoundaryColumn()).isEqualTo("slot");
        assertThat(pool.getSourceColumnMappings()).isEqualTo(Map.of("vrf_key_hash", "vrf_key"));
    }

    private static TableExporter exporterWith(PartitionStrategy strategy) {
        return new TableExporter() {
            @Override public String getTableName() { return "t"; }
            @Override public PartitionStrategy getPartitionStrategy() { return strategy; }
            @Override public String getPartitionColumn() { return "block_time"; }
            @Override public boolean exportForPartition(PartitionValue partition) { return true; }
        };
    }

    private static StorageWriter writer() {
        StorageWriter writer = mock(StorageWriter.class);
        when(writer.getSourceSchema()).thenReturn("mainnet");
        return writer;
    }
    private static ExportStateService stateService() { return mock(ExportStateService.class); }
    private static EraService eraService() { return mock(EraService.class); }
    private static AnalyticsStoreProperties props() { return new AnalyticsStoreProperties(); }
    private static AdaPotJobStorage adaPot() { return mock(AdaPotJobStorage.class); }
}
