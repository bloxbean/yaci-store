package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties.CustomExporterConfig;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomTableExporterTest {

    @Test
    void buildQuery_daily_shouldResolveSourceAndSlotPlaceholders() {
        CustomTableExporter exporter = createExporter(
                createConfig("my_table", "DAILY",
                        "SELECT * FROM {source}.tx WHERE slot >= {start_slot} AND slot < {end_slot}"));

        String sql = exporter.buildQuery(
                PartitionValue.ofDate(LocalDate.of(2024, 1, 15)),
                new SlotRange(10, 20));

        assertThat(sql).isEqualTo("SELECT * FROM source_db.mainnet.tx WHERE slot >= 10 AND slot < 20");
    }

    @Test
    void buildQuery_epoch_shouldResolveEpochPlaceholder() {
        CustomTableExporter exporter = createExporter(
                createConfig("epoch_table", "EPOCH",
                        "SELECT * FROM {source}.rewards WHERE epoch_no = {epoch}"));

        String sql = exporter.buildQuery(
                PartitionValue.ofEpoch(450),
                new SlotRange(100, 200));

        assertThat(sql).isEqualTo("SELECT * FROM source_db.mainnet.rewards WHERE epoch_no = 450");
    }

    @Test
    void buildQuery_epoch_shouldResolveAllPlaceholders() {
        CustomTableExporter exporter = createExporter(
                createConfig("full_table", "EPOCH",
                        "SELECT * FROM {source}.data WHERE slot >= {start_slot} AND slot < {end_slot} AND epoch = {epoch}"));

        String sql = exporter.buildQuery(
                PartitionValue.ofEpoch(450),
                new SlotRange(100, 200));

        assertThat(sql).isEqualTo(
                "SELECT * FROM source_db.mainnet.data WHERE slot >= 100 AND slot < 200 AND epoch = 450");
    }

    @Test
    void getTableName_shouldReturnConfigName() {
        CustomTableExporter exporter = createExporter(
                createConfig("custom_transactions", "DAILY", "SELECT 1"));

        assertThat(exporter.getTableName()).isEqualTo("custom_transactions");
    }

    @Test
    void getPartitionStrategy_shouldReturnDaily() {
        CustomTableExporter exporter = createExporter(
                createConfig("t", "DAILY", "SELECT 1"));

        assertThat(exporter.getPartitionStrategy()).isEqualTo(PartitionStrategy.DAILY);
    }

    @Test
    void getPartitionStrategy_shouldReturnEpoch() {
        CustomTableExporter exporter = createExporter(
                createConfig("t", "EPOCH", "SELECT 1"));

        assertThat(exporter.getPartitionStrategy()).isEqualTo(PartitionStrategy.EPOCH);
    }

    @Test
    void getPartitionColumn_shouldReturnCustomColumn() {
        CustomExporterConfig config = createConfig("t", "DAILY", "SELECT 1");
        config.setPartitionColumn("created_at");
        CustomTableExporter exporter = createExporter(config);

        assertThat(exporter.getPartitionColumn()).isEqualTo("created_at");
    }

    @Test
    void preExportValidation_dependsOnAdapotJobFalse_shouldReturnTrue() {
        CustomTableExporter exporter = createExporter(
                createConfig("t", "DAILY", "SELECT 1"));

        assertThat(exporter.preExportValidation(PartitionValue.ofDate(LocalDate.of(2024, 1, 15)))).isTrue();
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(450))).isTrue();
    }

    @Test
    void preExportValidation_dependsOnAdapotJobTrue_withDatePartition_shouldReturnTrue() {
        CustomExporterConfig config = createConfig("t", "DAILY", "SELECT 1");
        config.setDependsOnAdapotJob(true);
        CustomTableExporter exporter = createExporter(config);

        assertThat(exporter.preExportValidation(PartitionValue.ofDate(LocalDate.of(2024, 1, 15)))).isTrue();
    }

    @Test
    void configDefaults_shouldHaveExpectedValues() {
        CustomExporterConfig config = new CustomExporterConfig();

        assertThat(config.getPartitionStrategy()).isEqualTo("DAILY");
        assertThat(config.getPartitionColumn()).isEqualTo("block_time");
        assertThat(config.isDependsOnAdapotJob()).isFalse();
    }

    private CustomExporterConfig createConfig(String name, String strategy, String query) {
        CustomExporterConfig config = new CustomExporterConfig();
        config.setName(name);
        config.setPartitionStrategy(strategy);
        config.setQuery(query);
        return config;
    }

    private CustomTableExporter createExporter(CustomExporterConfig config) {
        StorageWriter storageWriter = mock(StorageWriter.class);
        when(storageWriter.getSourceSchema()).thenReturn("mainnet");

        ExportStateService stateService = mock(ExportStateService.class);
        EraService eraService = mock(EraService.class);
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        AdaPotJobStorage adaPotJobStorage = mock(AdaPotJobStorage.class);

        return new CustomTableExporter(config, storageWriter, stateService, eraService, properties, adaPotJobStorage);
    }
}
