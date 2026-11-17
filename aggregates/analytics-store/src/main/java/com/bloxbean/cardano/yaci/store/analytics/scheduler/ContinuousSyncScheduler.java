package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.config.PruningSafetyCheck;
import com.bloxbean.cardano.yaci.store.analytics.exporter.NoOpAdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler for continuous sync during blockchain synchronization.
 *
 * Automatically runs when analytics.enabled=true to detect and fill export gaps.
 * Exports missing dates sequentially from oldest to newest for all enabled daily tables.
 *
 * Uses TableExporterRegistry to automatically discover and export all daily and epoch tables
 * that are enabled in configuration. A missing partition that is not ready (for example an
 * epoch whose AdaPot job has not run) is not a gap, and a partition whose export failed is
 * retried with an exponential backoff.
 *
 * Uses adaptive scheduling: short interval (default 1 min) while catching up,
 * standard interval (default 15 min) once fully synced.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ContinuousSyncScheduler {

    private final GapDetectionService gapDetectionService;
    private final UniversalExportService universalExportService;
    private final TableExporterRegistry registry;
    private final AnalyticsStoreProperties properties;
    private final TaskScheduler taskScheduler;
    private final CursorService cursorService;
    private final AdaPotJobStorage adaPotJobStorage;

    private volatile boolean lastRunHadGaps = true; // start optimistic
    /** Single flight: the scheduled run and POST /sync/trigger never sync at the same time. */
    private final AtomicBoolean syncRunning = new AtomicBoolean();
    /** Last state logged at INFO, so an unchanged state is logged at DEBUG only. */
    private volatile String lastLoggedState;
    /** Holds PostgreSQL pruning while the export is behind; absent in unit tests. */
    private ObjectProvider<PruningSafetyCheck> pruningGuard;

    @Autowired(required = false)
    void setPruningGuard(ObjectProvider<PruningSafetyCheck> pruningGuard) {
        this.pruningGuard = pruningGuard;
    }

    private void holdPruning(boolean behind) {
        PruningSafetyCheck guard = pruningGuard == null ? null : pruningGuard.getIfAvailable();
        if (guard != null) {
            guard.setHeld(behind);
        }
    }

    public ContinuousSyncScheduler(GapDetectionService gapDetectionService,
                                   UniversalExportService universalExportService,
                                   TableExporterRegistry registry,
                                   AnalyticsStoreProperties properties,
                                   TaskScheduler taskScheduler,
                                   CursorService cursorService,
                                   AdaPotJobStorage adaPotJobStorage) {
        this.gapDetectionService = gapDetectionService;
        this.universalExportService = universalExportService;
        this.registry = registry;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.cursorService = cursorService;
        this.adaPotJobStorage = adaPotJobStorage;
    }

    @PostConstruct
    public void init() {
        scheduleNext(Duration.ofSeconds(30));
        log.info("Continuous sync scheduler initialized (catch-up: {}min, normal: {}min)",
                properties.getContinuousSync().getCatchUpIntervalMinutes(),
                properties.getContinuousSync().getSyncCheckIntervalMinutes());
    }

    private void scheduleNext(Duration delay) {
        taskScheduler.schedule(this::runAndReschedule, Instant.now().plus(delay));
    }

    private void runAndReschedule() {
        try {
            syncGaps();
        } finally {
            boolean hasGaps = lastRunHadGaps;
            int intervalMinutes = hasGaps
                    ? properties.getContinuousSync().getCatchUpIntervalMinutes()
                    : properties.getContinuousSync().getSyncCheckIntervalMinutes();
            log.debug("Next sync in {} minutes ({})", intervalMinutes,
                    hasGaps ? "catching up" : "fully synced");
            scheduleNext(Duration.ofMinutes(intervalMinutes));
        }
    }

    /** Whether a sync (scheduled or triggered) is running now. */
    public boolean isSyncRunning() {
        return syncRunning.get();
    }

    /**
     * Check for gaps and export the missing partitions that are ready, for all enabled tables.
     *
     * <p>Only one sync runs at a time. A call while another sync is running (the scheduled run
     * and {@code POST /sync/trigger}) returns {@code false} at once and does nothing.</p>
     *
     * @return true if this call ran the sync, false if another sync was already running
     */
    public boolean syncGaps() {
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("Analytics sync is already running; not starting another one");
            return false;
        }
        try {
            runSync();
            return true;
        } finally {
            syncRunning.set(false);
        }
    }

    private void runSync() {
        // Skip exports if sync is still catching up (block range sync)
        if (properties.getContinuousSync().isExportAfterSync() && !cursorService.isSyncMode()) {
            logState("deferred-sync", "Skipping analytics export — yaci store is still syncing. "
                    + "Exports will begin once sync reaches chain tip.");
            lastRunHadGaps = true; // keep catch-up interval for quick start after tip
            holdPruning(true);   // nothing is exported yet, so pruning would delete history for good
            return;
        }

        // Skip exports if any adapot job is running
        if (!(adaPotJobStorage instanceof NoOpAdaPotJobStorage)) {
            List<AdaPotJob> runningJobs = adaPotJobStorage.getJobsByTypeAndStatus(
                    AdaPotJobType.REWARD_CALC, AdaPotJobStatus.STARTED);
            if (!runningJobs.isEmpty()) {
                logState("adapot-running", String.format("Skipping analytics export — AdaPot job in progress "
                        + "(epoch %d). Will retry next iteration.", runningJobs.get(0).getEpoch()));
                lastRunHadGaps = true; // keep catch-up interval
                return;
            }
        }

        try {
            Gaps gaps = findGaps();
            logState("gaps:" + gaps.summary(), "Analytics export gaps: " + gaps.summary());
            // A day that is ready but not exported yet (missing, or a failed export waiting for its
            // retry) must keep its PostgreSQL rows until it is exported
            holdPruning(gaps.readyMissingDates > 0);
            if (gaps.due.isEmpty()) {
                lastRunHadGaps = false;
                return;
            }
            exportDue(gaps.due);
            lastRunHadGaps = true;
        } catch (Exception e) {
            log.error("Continuous sync scheduler encountered error: {}", e.getMessage(), e);
        }
    }

    /**
     * Gaps of every enabled table. A partition that is not ready (for example waiting for an
     * AdaPot job) is not a gap, and a partition whose last export failed waits for its retry.
     */
    private Gaps findGaps() {
        Map<PartitionValue, List<String>> due = new TreeMap<>(PARTITION_ORDER);
        Set<LocalDate> readyMissingDates = new HashSet<>();
        Set<Integer> readyMissingEpochs = new HashSet<>();
        int backingOff = 0;
        int notReady = 0;
        int dailyTables = 0;
        int epochTables = 0;

        for (PartitionStrategy strategy : List.of(PartitionStrategy.DAILY, PartitionStrategy.EPOCH)) {
            // Tables that cannot be exported with this configuration (e.g. AdaPot tables with adapot off) are not gaps
            for (String tableName : registry.getExportableTablesByStrategy(strategy)) {
                if (strategy == PartitionStrategy.DAILY) {
                    dailyTables++;
                } else {
                    epochTables++;
                }
                GapDetectionService.TableGaps tableGaps = gapDetectionService.findGaps(registry.getExporter(tableName));
                for (PartitionValue partition : tableGaps.due()) {
                    due.computeIfAbsent(partition, p -> new ArrayList<>()).add(tableName);
                }
                for (List<PartitionValue> readyMissing : List.of(tableGaps.due(), tableGaps.backingOff())) {
                    for (PartitionValue partition : readyMissing) {
                        if (partition instanceof PartitionValue.DatePartition date) {
                            readyMissingDates.add(date.date());
                        } else if (partition instanceof PartitionValue.EpochPartition epoch) {
                            readyMissingEpochs.add(epoch.epoch());
                        }
                    }
                }
                backingOff += tableGaps.backingOff().size();
                notReady += tableGaps.notReady().size();
            }
        }
        return new Gaps(due, readyMissingDates.size(), readyMissingEpochs.size(), backingOff, notReady,
                dailyTables, epochTables);
    }

    /** Dates first (oldest first), then epochs (oldest first). */
    private static final Comparator<PartitionValue> PARTITION_ORDER = Comparator
            .comparingInt((PartitionValue p) -> p instanceof PartitionValue.DatePartition ? 0 : 1)
            .thenComparingLong(p -> p instanceof PartitionValue.DatePartition d ? d.date().toEpochDay()
                    : (long) ((PartitionValue.EpochPartition) p).epoch());

    /** Exports the due partitions, oldest first, each only for the tables that miss it. */
    private void exportDue(Map<PartitionValue, List<String>> due) {
        int exports = due.values().stream().mapToInt(List::size).sum();
        log.info("Exporting {} missing partitions ({} distinct dates/epochs)", exports, due.size());

        int successCount = 0;
        int failureCount = 0;
        int notReadyCount = 0;
        for (Map.Entry<PartitionValue, List<String>> entry : due.entrySet()) {
            for (String tableName : entry.getValue()) {
                try {
                    switch (universalExportService.exportTable(tableName, entry.getKey()).outcome()) {
                        case EXPORTED -> successCount++;
                        // Became not ready between gap detection and the export (e.g. a rollback); nothing was marked
                        case NOT_READY -> notReadyCount++;
                        case FAILED -> failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to export {} for {}: {}", tableName, entry.getKey().toPathSegment(),
                            e.getMessage(), e);
                    failureCount++;
                }
            }
        }

        log.info("Sync batch complete: {} exports successful, {} failed, {} not ready", successCount, failureCount,
                notReadyCount);
    }

    /** Logs at INFO when the state differs from the last one logged at INFO, otherwise at DEBUG. */
    private void logState(String state, String message) {
        if (!state.equals(lastLoggedState)) {
            lastLoggedState = state;
            log.info(message);
        } else {
            log.debug(message);
        }
    }

    /**
     * @param due                partition → tables that miss it and can export it now
     * @param readyMissingDates  distinct dates some table could export but has not
     * @param readyMissingEpochs distinct epochs some table could export but has not
     * @param backingOff         (table, partition) pairs waiting for a retry after a failure
     * @param notReady           (table, partition) pairs missing but not exportable yet
     */
    private record Gaps(Map<PartitionValue, List<String>> due, int readyMissingDates, int readyMissingEpochs,
                        int backingOff, int notReady, int dailyTables, int epochTables) {
        String summary() {
            return String.format("%d dates and %d epochs missing (%d table partitions waiting for a retry), "
                            + "%d table partitions not ready yet", readyMissingDates, readyMissingEpochs, backingOff,
                    notReady);
        }
    }

    /**
     * Get current sync status for monitoring.
     *
     * Aggregates status across all enabled daily and epoch tables. Only partitions that are
     * ready to be exported count as missing; {@code fullySynced} is true when none is.
     *
     * @return SyncStatus with current sync information
     */
    public SyncStatus getSyncStatus() {
        LocalDate genesis = gapDetectionService.getGenesisDate();
        LocalDate latestSynced = gapDetectionService.getLatestSyncedDate();
        LocalDate exportEndDate = gapDetectionService.getExportEndDate();

        Gaps gaps = findGaps();
        boolean fullySynced = gaps.readyMissingDates == 0 && gaps.readyMissingEpochs == 0;
        boolean exportDeferred = properties.getContinuousSync().isExportAfterSync()
                && !cursorService.isSyncMode();

        return SyncStatus.builder()
            .genesisDate(genesis)
            .latestSyncedDate(latestSynced)
            .exportEndDate(exportEndDate)
            .bufferDays(properties.getContinuousSync().getBufferDays())
            .finalityWindowHours(gapDetectionService.getFinalityWindow().toMinutes() / 60.0)
            .missingExportCount(gaps.readyMissingDates)
            .isFullySynced(fullySynced)
            .enabledTableCount(gaps.dailyTables)
            .missingEpochExportCount(gaps.readyMissingEpochs)
            .enabledEpochTableCount(gaps.epochTables)
            .exportDeferred(exportDeferred)
            .retryPendingCount(gaps.backingOff)
            .notReadyCount(gaps.notReady)
            .syncRunning(syncRunning.get())
            .build();
    }

    /**
     * Sync status information for monitoring and admin API.
     */
    @Data
    @Builder
    public static class SyncStatus {
        private LocalDate genesisDate;
        private LocalDate latestSyncedDate;
        private LocalDate exportEndDate;
        private int bufferDays;
        /** Finality window incl. margin (hours) subtracted from the tip before buffer-days is applied. */
        private double finalityWindowHours;
        private int missingExportCount;
        private boolean isFullySynced;
        private int enabledTableCount;
        private int missingEpochExportCount;
        private int enabledEpochTableCount;
        private boolean exportDeferred;
        /** (table, partition) pairs whose export failed and that wait for their next retry. */
        private int retryPendingCount;
        /** (table, partition) pairs that are missing but not exportable yet (not counted as missing). */
        private int notReadyCount;
        /** Whether a sync is running now. */
        private boolean syncRunning;
    }
}
