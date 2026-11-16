package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.NoOpAdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService.TableGaps;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContinuousSyncSchedulerTest {

    private static final PartitionValue DAY = PartitionValue.ofDate(LocalDate.of(2024, 3, 1));

    private GapDetectionService gapDetection;
    private UniversalExportService exportService;
    private TableExporterRegistry registry;
    private ContinuousSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        gapDetection = mock(GapDetectionService.class);
        exportService = mock(UniversalExportService.class);
        registry = mock(TableExporterRegistry.class);
        CursorService cursorService = mock(CursorService.class);
        when(cursorService.isSyncMode()).thenReturn(true);
        when(gapDetection.getFinalityWindow()).thenReturn(Duration.ofHours(13));
        scheduler = new ContinuousSyncScheduler(gapDetection, exportService, registry, new AnalyticsStoreProperties(),
                mock(TaskScheduler.class), cursorService, new NoOpAdaPotJobStorage());
    }

    @Test
    void partitionsThatAreNotReadyAreNotGapsAndTheStoreIsFullySynced() {
        TableExporter reward = exporter("reward", PartitionStrategy.EPOCH);
        tables(List.of(), List.of(reward));
        when(gapDetection.findGaps(reward)).thenReturn(new TableGaps("reward", List.of(), List.of(),
                List.of(PartitionValue.ofEpoch(499), PartitionValue.ofEpoch(500))));

        assertThat(scheduler.syncGaps()).isTrue();

        verify(exportService, never()).exportTable(any(), any());
        ContinuousSyncScheduler.SyncStatus status = scheduler.getSyncStatus();
        assertThat(status.isFullySynced()).isTrue();
        assertThat(status.getMissingEpochExportCount()).isZero();
        assertThat(status.getNotReadyCount()).isEqualTo(2);
    }

    @Test
    void aPartitionWaitingForItsRetryIsMissingButNotExported() {
        TableExporter block = exporter("block", PartitionStrategy.DAILY);
        tables(List.of(block), List.of());
        when(gapDetection.findGaps(block)).thenReturn(new TableGaps("block", List.of(), List.of(DAY), List.of()));

        scheduler.syncGaps();

        verify(exportService, never()).exportTable(any(), any());
        ContinuousSyncScheduler.SyncStatus status = scheduler.getSyncStatus();
        assertThat(status.isFullySynced()).isFalse();
        assertThat(status.getMissingExportCount()).isEqualTo(1);
        assertThat(status.getRetryPendingCount()).isEqualTo(1);
    }

    @Test
    void onlyTheTablesMissingAPartitionAreExported() {
        TableExporter block = exporter("block", PartitionStrategy.DAILY);
        TableExporter transaction = exporter("transaction", PartitionStrategy.DAILY);
        tables(List.of(block, transaction), List.of());
        when(gapDetection.findGaps(block)).thenReturn(new TableGaps("block", List.of(), List.of(), List.of()));
        when(gapDetection.findGaps(transaction)).thenReturn(new TableGaps("transaction", List.of(DAY), List.of(),
                List.of()));
        when(exportService.exportTable("transaction", DAY)).thenReturn(exported("transaction"));

        scheduler.syncGaps();

        verify(exportService, times(1)).exportTable("transaction", DAY);
        verify(exportService, never()).exportTable(eq("block"), any());
        verify(exportService, never()).exportAllDailyTables(any());
    }

    @Test
    void aSecondSyncWhileOneIsRunningReturnsAtOnce() throws Exception {
        TableExporter block = exporter("block", PartitionStrategy.DAILY);
        tables(List.of(block), List.of());
        when(gapDetection.findGaps(block)).thenReturn(new TableGaps("block", List.of(DAY), List.of(), List.of()));
        CountDownLatch exporting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(exportService.exportTable("block", DAY)).thenAnswer(inv -> {
            exporting.countDown();
            release.await(10, TimeUnit.SECONDS);
            return exported("block");
        });

        CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(scheduler::syncGaps);
        assertThat(exporting.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(scheduler.isSyncRunning()).isTrue();

        assertThat(scheduler.syncGaps()).isFalse();

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(scheduler.isSyncRunning()).isFalse();
        verify(exportService, times(1)).exportTable("block", DAY);
    }

    private void tables(List<TableExporter> daily, List<TableExporter> epoch) {
        List<String> dailyNames = daily.stream().map(TableExporter::getTableName).toList();
        List<String> epochNames = epoch.stream().map(TableExporter::getTableName).toList();
        when(registry.getExportableTablesByStrategy(PartitionStrategy.DAILY)).thenReturn(dailyNames);
        when(registry.getExportableTablesByStrategy(PartitionStrategy.EPOCH)).thenReturn(epochNames);
        for (TableExporter exporter : daily) {
            String name = exporter.getTableName();
            when(registry.getExporter(name)).thenReturn(exporter);
        }
        for (TableExporter exporter : epoch) {
            String name = exporter.getTableName();
            when(registry.getExporter(name)).thenReturn(exporter);
        }
    }

    private static TableExporter exporter(String table, PartitionStrategy strategy) {
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getTableName()).thenReturn(table);
        when(exporter.getPartitionStrategy()).thenReturn(strategy);
        return exporter;
    }

    private static UniversalExportService.PartitionExportResult exported(String table) {
        return new UniversalExportService.PartitionExportResult(table, DAY.toPathSegment(),
                UniversalExportService.Outcome.EXPORTED, PartitionReadiness.READY);
    }
}
