package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EpochExporterTest {

    private final BlockStorageReader blocks = mock(BlockStorageReader.class);
    private final EpochAggregateReader epochRows = mock(EpochAggregateReader.class);
    private final ExportStateService stateService = mock(ExportStateService.class);
    private EpochExporter exporter;

    @BeforeEach
    void setUp() {
        when(epochRows.isEpochCalculationEnabled()).thenReturn(true);
        when(blocks.findRecentBlock()).thenReturn(Optional.of(Block.builder().epochNumber(501).build()));
        exporter = new EpochExporter(mock(StorageWriter.class), stateService, mock(EraService.class),
                new AnalyticsStoreProperties(), mock(AdaPotJobStorage.class), blocks, epochRows);
    }

    @Test
    void isUnavailableAndExportsNothingWithoutEpochCalculation() {
        when(epochRows.isEpochCalculationEnabled()).thenReturn(false);

        assertThat(exporter.unavailableReason())
                .hasValueSatisfying(r -> assertThat(r).contains("store.epoch-aggr.epoch-calculation-enabled"));
        assertThat(exporter.exportForPartition(PartitionValue.ofEpoch(500))).isFalse();
        verify(stateService, never()).markInProgress(anyString(), anyString());
    }

    @Test
    void theCurrentEpochIsNotReady() {
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(501))).isFalse();
    }

    @Test
    void noEpochIsReadyBeforeTheFirstBlock() {
        when(blocks.findRecentBlock()).thenReturn(Optional.empty());

        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(0))).isFalse();
    }

    @Test
    void aPastEpochWaitsUntilItsRowCountsEveryBlock() {
        when(blocks.totalBlocksInEpoch(500)).thenReturn(21600);

        when(epochRows.findBlockCount(500)).thenReturn(OptionalInt.empty());
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(500))).isFalse();

        when(epochRows.findBlockCount(500)).thenReturn(OptionalInt.of(21000));
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(500))).isFalse();

        when(epochRows.findBlockCount(500)).thenReturn(OptionalInt.of(21600));
        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(500))).isTrue();
    }

    @Test
    void aPastEpochWithoutBlocksIsReady() {
        when(blocks.totalBlocksInEpoch(10)).thenReturn(0);

        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(10))).isTrue();
    }

    @Test
    void anErrorMeansNotReady() {
        when(blocks.totalBlocksInEpoch(500)).thenReturn(21600);
        when(epochRows.findBlockCount(500)).thenThrow(new RuntimeException("relation \"epoch\" does not exist"));

        assertThat(exporter.preExportValidation(PartitionValue.ofEpoch(500))).isFalse();
    }

    @Test
    void epochCalculationNeedsBothEpochAggrProperties() {
        DataSource dataSource = mock(DataSource.class);
        assertThat(new EpochAggregateReader(dataSource, new MockEnvironment()).isEpochCalculationEnabled()).isFalse();
        assertThat(new EpochAggregateReader(dataSource, new MockEnvironment()
                .withProperty("store.epoch-aggr.epoch-calculation-enabled", "true"))
                .isEpochCalculationEnabled()).isFalse();
        assertThat(new EpochAggregateReader(dataSource, new MockEnvironment()
                .withProperty("store.epoch-aggr.enabled", "true"))
                .isEpochCalculationEnabled()).isFalse();
        assertThat(new EpochAggregateReader(dataSource, new MockEnvironment()
                .withProperty("store.epoch-aggr.enabled", "true")
                .withProperty("store.epoch-aggr.epoch-calculation-enabled", "true"))
                .isEpochCalculationEnabled()).isTrue();
    }
}
