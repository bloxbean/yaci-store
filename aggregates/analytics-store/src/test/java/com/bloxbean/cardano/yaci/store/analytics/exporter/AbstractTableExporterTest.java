package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStatus;
import com.bloxbean.cardano.yaci.store.analytics.writer.ExportResult;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractTableExporterTest {

    private static final PartitionValue EPOCH_500 = PartitionValue.ofEpoch(500);

    private final StorageWriter writer = mock(StorageWriter.class);
    private final ExportStateService stateService = mock(ExportStateService.class);
    private final EraService eraService = mock(EraService.class);
    private final AdaPotJobStorage jobs = mock(AdaPotJobStorage.class);
    private final AnalyticsStoreProperties properties = new AnalyticsStoreProperties();

    @BeforeEach
    void setUp() {
        when(writer.getStorageFormat()).thenReturn("DUCKLAKE");
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(208));
        when(eraService.getShelleyAbsoluteSlot(any(Integer.class), any(Integer.class)))
                .thenAnswer(i -> (long) (int) i.getArgument(0) * 432000L);
    }

    // ---- skip and verify paths ----

    @Test
    void skipsACompletedPartitionWhoseDataIsPresent() {
        when(stateService.getState("t", "epoch=500")).thenReturn(completed(10));
        when(writer.verifyExport(anyString(), eq(10L))).thenReturn(true);

        assertThat(exporter(false, jobs).exportForPartition(EPOCH_500)).isTrue();

        verify(stateService, never()).markInProgress(anyString(), anyString());
        verify(writer, never()).export(anyString(), anyString(), anyString());
    }

    @Test
    void reExportsACompletedPartitionWhoseDataIsMissing() {
        when(stateService.getState("t", "epoch=500")).thenReturn(completed(10));
        when(writer.verifyExport(anyString(), anyLong())).thenReturn(false, true);
        ExportState inProgress = new ExportState("t", "epoch=500");
        when(stateService.markInProgress("t", "epoch=500")).thenReturn(inProgress);
        ExportResult result = new ExportResult("p", 10, 100, 1);
        when(writer.export(anyString(), anyString(), anyString())).thenReturn(result);

        assertThat(exporter(false, jobs).exportForPartition(EPOCH_500)).isTrue();

        verify(stateService).markCompleted(inProgress, result, null);
    }

    @Test
    void marksFailedWhenTheWriterReportsRowsThatAreNotOnStorage() {
        ExportState inProgress = new ExportState("t", "epoch=500");
        when(stateService.markInProgress("t", "epoch=500")).thenReturn(inProgress);
        when(writer.export(anyString(), anyString(), anyString())).thenReturn(new ExportResult("p", 10, 100, 1));
        when(writer.verifyExport(anyString(), eq(10L))).thenReturn(false);

        assertThat(exporter(false, jobs).exportForPartition(EPOCH_500)).isFalse();

        verify(stateService).markFailed(eq(inProgress), anyString());
        verify(stateService, never()).markCompleted(any(), any(), any());
    }

    @Test
    void doesNotTouchTheStateWhenValidationFails() {
        TestExporter exporter = exporter(false, jobs);
        exporter.valid = false;

        assertThat(exporter.exportForPartition(EPOCH_500)).isFalse();

        verify(stateService, never()).markInProgress(anyString(), anyString());
    }

    @Test
    void leavesThePartitionToTheOtherExportWhenMarkingInProgressFails() {
        when(stateService.markInProgress("t", "epoch=500"))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(exporter(false, jobs).exportForPartition(EPOCH_500)).isFalse();

        verify(writer, never()).export(anyString(), anyString(), anyString());
        verify(stateService, never()).markFailed(any(), anyString());
    }

    @Test
    void anEmptyResultIsFailedForATableThatAlwaysHasRows() {
        ExportState inProgress = new ExportState("t", "epoch=500");
        when(stateService.markInProgress("t", "epoch=500")).thenReturn(inProgress);
        when(writer.export(anyString(), anyString(), anyString())).thenReturn(new ExportResult("p", 0, 0, 1));
        TestExporter exporter = exporter(false, jobs);
        exporter.allowsEmpty = false;

        assertThat(exporter.exportForPartition(EPOCH_500)).isFalse();

        verify(stateService).markFailed(eq(inProgress), org.mockito.ArgumentMatchers.contains("no rows"));
        verify(stateService, never()).markCompleted(any(), any(), any());
    }

    @Test
    void anEmptyResultIsCompletedForOtherTables() {
        ExportState inProgress = new ExportState("t", "epoch=500");
        when(stateService.markInProgress("t", "epoch=500")).thenReturn(inProgress);
        ExportResult empty = new ExportResult("p", 0, 0, 1);
        when(writer.export(anyString(), anyString(), anyString())).thenReturn(empty);

        assertThat(exporter(false, jobs).exportForPartition(EPOCH_500)).isTrue();

        verify(stateService).markCompleted(inProgress, empty, null);
    }

    // ---- AdaPot dependency ----

    @Test
    void anAdaPotTableIsUnavailableAndExportsNothingWhenAdaPotIsDisabled() {
        TestExporter exporter = exporter(true, new NoOpAdaPotJobStorage());

        assertThat(exporter.unavailableReason()).hasValueSatisfying(r -> assertThat(r).contains("store.adapot.enabled"));
        assertThat(exporter.exportForPartition(EPOCH_500)).isFalse();
        verify(stateService, never()).markInProgress(anyString(), anyString());
    }

    @Test
    void otherTablesStayAvailableWhenAdaPotIsDisabled() {
        assertThat(exporter(false, new NoOpAdaPotJobStorage()).unavailableReason()).isEmpty();
        assertThat(exporter(true, jobs).unavailableReason()).isEmpty();
    }

    @Test
    void adaPotCheckFailsClosedOnAnError() {
        when(jobs.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, 500))
                .thenThrow(new RuntimeException("connection refused"));

        assertThat(exporter(true, jobs).isRewardCalcAdaPotJobCompleted(500)).isFalse();
    }

    @Test
    void adaPotCheckIsNotReadyWhileTheChainIsStillInByron() {
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.empty());

        assertThat(exporter(true, jobs).isRewardCalcAdaPotJobCompleted(5)).isFalse();
        assertThat(exporter(true, jobs).isRewardCalcAdaPotJobCompleted(500)).isFalse();
    }

    @Test
    void adaPotCheckPassesForAFinishedByronEpochAndFollowsTheJobOtherwise() {
        TestExporter exporter = exporter(true, jobs);
        assertThat(exporter.isRewardCalcAdaPotJobCompleted(100)).isTrue();

        assertThat(exporter.isRewardCalcAdaPotJobCompleted(500)).isFalse(); // no job yet
        job(500, AdaPotJobStatus.STARTED);
        assertThat(exporter.isRewardCalcAdaPotJobCompleted(500)).isFalse();
        job(500, AdaPotJobStatus.COMPLETED);
        assertThat(exporter.isRewardCalcAdaPotJobCompleted(500)).isTrue();
    }

    // ---- registry ----

    @Test
    void registryLeavesUnavailableExportersOutOfTheExportableTables() {
        TableExporterRegistry registry = new TableExporterRegistry(properties);
        registry.registerExporter(exporter("reward_like", true, new NoOpAdaPotJobStorage()));
        registry.registerExporter(exporter("epoch_like", false, new NoOpAdaPotJobStorage()));

        assertThat(registry.getEnabledTablesByStrategy(PartitionStrategy.EPOCH))
                .containsExactly("epoch_like", "reward_like");
        assertThat(registry.getExportableTablesByStrategy(PartitionStrategy.EPOCH))
                .containsExactly("epoch_like");
    }

    private void job(int epoch, AdaPotJobStatus status) {
        AdaPotJob job = new AdaPotJob();
        job.setEpoch(epoch);
        job.setType(AdaPotJobType.REWARD_CALC);
        job.setStatus(status);
        when(jobs.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch)).thenReturn(Optional.of(job));
    }

    private static ExportState completed(long rows) {
        ExportState state = new ExportState("t", "epoch=500");
        state.setStatus(ExportStatus.COMPLETED);
        state.setRowCount(rows);
        state.setStartedAt(LocalDateTime.now());
        return state;
    }

    private TestExporter exporter(boolean dependsOnAdaPot, AdaPotJobStorage storage) {
        return exporter("t", dependsOnAdaPot, storage);
    }

    private TestExporter exporter(String name, boolean dependsOnAdaPot, AdaPotJobStorage storage) {
        return new TestExporter(name, dependsOnAdaPot, writer, stateService, eraService, properties, storage);
    }

    static class TestExporter extends AbstractTableExporter {
        private final String name;
        private final boolean dependsOnAdaPot;
        boolean valid = true;
        boolean allowsEmpty = true;

        TestExporter(String name, boolean dependsOnAdaPot, StorageWriter writer, ExportStateService stateService,
                     EraService eraService, AnalyticsStoreProperties properties, AdaPotJobStorage storage) {
            super(writer, stateService, eraService, properties, storage);
            this.name = name;
            this.dependsOnAdaPot = dependsOnAdaPot;
        }

        @Override
        public String getTableName() {
            return name;
        }

        @Override
        public PartitionStrategy getPartitionStrategy() {
            return PartitionStrategy.EPOCH;
        }

        @Override
        protected boolean dependsOnAdaPotJob() {
            return dependsOnAdaPot;
        }

        @Override
        public boolean preExportValidation(PartitionValue partition) {
            return valid;
        }

        @Override
        protected boolean allowsEmptyPartition(PartitionValue partition) {
            return allowsEmpty;
        }

        @Override
        protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
            return "SELECT 1";
        }
    }
}
