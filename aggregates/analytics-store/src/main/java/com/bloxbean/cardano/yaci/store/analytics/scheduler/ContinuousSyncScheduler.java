package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Scheduler for continuous sync during blockchain synchronization.
 *
 * Automatically runs when analytics.enabled=true to detect and fill export gaps.
 * Exports missing dates sequentially from oldest to newest for all enabled daily tables.
 *
 * Uses TableExporterRegistry to automatically discover and export all daily tables
 * that are enabled in configuration.
 *
 * Uses adaptive scheduling: short interval (default 1 min) while catching up,
 * standard interval (default 15 min) once fully synced.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ContinuousSyncScheduler {

    private final GapDetectionService gapDetectionService;
    private final UniversalExportScheduler universalExportScheduler;
    private final TableExporterRegistry registry;
    private final AnalyticsStoreProperties properties;
    private final TaskScheduler taskScheduler;

    private volatile boolean lastRunHadGaps = true; // start optimistic

    public ContinuousSyncScheduler(GapDetectionService gapDetectionService,
                                   UniversalExportScheduler universalExportScheduler,
                                   TableExporterRegistry registry,
                                   AnalyticsStoreProperties properties,
                                   TaskScheduler taskScheduler) {
        this.gapDetectionService = gapDetectionService;
        this.universalExportScheduler = universalExportScheduler;
        this.registry = registry;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
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

    /**
     * Periodically check for gaps and export missing partitions for all enabled tables.
     *
     * Handles both DAILY and EPOCH partition strategies. Finds the union of missing
     * partitions across all enabled tables and exports them sequentially.
     */
    public void syncGaps() {
        log.debug("Running continuous sync gap detection for all tables");

        try {
            boolean dailyHadGaps = syncDailyGaps();
            boolean epochHadGaps = syncEpochGaps();
            lastRunHadGaps = dailyHadGaps || epochHadGaps;
        } catch (Exception e) {
            log.error("Continuous sync scheduler encountered error: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync gaps for all enabled DAILY tables.
     *
     * @return true if gaps were found
     */
    private boolean syncDailyGaps() {
        List<String> enabledDailyTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);

        if (enabledDailyTables.isEmpty()) {
            log.debug("No daily tables enabled, skipping daily sync");
            return false;
        }

        log.debug("Checking gaps for {} enabled daily tables: {}",
                enabledDailyTables.size(), enabledDailyTables);

        Set<LocalDate> allMissingDates = new HashSet<>();
        for (String tableName : enabledDailyTables) {
            List<LocalDate> tableMissingDates = gapDetectionService.findMissingExports(tableName);
            allMissingDates.addAll(tableMissingDates);
            if (!tableMissingDates.isEmpty()) {
                log.debug("Table {} has {} missing exports", tableName, tableMissingDates.size());
            }
        }

        if (allMissingDates.isEmpty()) {
            log.debug("No gaps detected across all daily tables, sync is up to date");
            return false;
        }

        List<LocalDate> sortedMissingDates = allMissingDates.stream()
                .sorted()
                .toList();

        log.info("Found {} unique missing dates across all daily tables, starting sequential export",
                sortedMissingDates.size());

        int successCount = 0;
        int failureCount = 0;

        for (LocalDate date : sortedMissingDates) {
            try {
                log.info("Exporting all daily tables for missing date: {}", date);
                universalExportScheduler.exportAllDailyTables(date);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to export all daily tables for {}: {}", date, e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Daily sync batch complete: {} dates successful, {} failed",
            successCount, failureCount);

        return true;
    }

    /**
     * Sync gaps for all enabled EPOCH tables.
     *
     * @return true if gaps were found
     */
    private boolean syncEpochGaps() {
        List<String> enabledEpochTables = registry.getEnabledTablesByStrategy(PartitionStrategy.EPOCH);

        if (enabledEpochTables.isEmpty()) {
            log.debug("No epoch tables enabled, skipping epoch sync");
            return false;
        }

        log.debug("Checking gaps for {} enabled epoch tables: {}",
                enabledEpochTables.size(), enabledEpochTables);

        Set<Integer> allMissingEpochs = new TreeSet<>();
        for (String tableName : enabledEpochTables) {
            List<Integer> tableMissingEpochs = gapDetectionService.findMissingEpochExports(tableName);
            allMissingEpochs.addAll(tableMissingEpochs);
            if (!tableMissingEpochs.isEmpty()) {
                log.debug("Table {} has {} missing epoch exports", tableName, tableMissingEpochs.size());
            }
        }

        if (allMissingEpochs.isEmpty()) {
            log.debug("No gaps detected across all epoch tables, sync is up to date");
            return false;
        }

        // TreeSet is already sorted
        log.info("Found {} unique missing epochs across all epoch tables, starting sequential export",
                allMissingEpochs.size());

        int successCount = 0;
        int failureCount = 0;

        for (int epoch : allMissingEpochs) {
            try {
                log.info("Exporting all epoch tables for missing epoch: {}", epoch);
                universalExportScheduler.exportAllEpochTables(epoch);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to export all epoch tables for epoch {}: {}", epoch, e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Epoch sync batch complete: {} epochs successful, {} failed",
            successCount, failureCount);

        return true;
    }

    /**
     * Get current sync status for monitoring.
     *
     * Aggregates status across all enabled daily and epoch tables.
     *
     * @return SyncStatus with current sync information
     */
    public SyncStatus getSyncStatus() {
        LocalDate genesis = gapDetectionService.getGenesisDate();
        LocalDate latestSynced = gapDetectionService.getLatestSyncedDate();
        LocalDate exportEndDate = gapDetectionService.getExportEndDate();

        // Get enabled daily tables from registry
        List<String> enabledDailyTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);

        // Find union of missing dates across all enabled daily tables
        Set<LocalDate> allMissingDates = new HashSet<>();
        for (String tableName : enabledDailyTables) {
            List<LocalDate> tableMissingDates = gapDetectionService.findMissingExports(tableName);
            allMissingDates.addAll(tableMissingDates);
        }

        // Get enabled epoch tables from registry
        List<String> enabledEpochTables = registry.getEnabledTablesByStrategy(PartitionStrategy.EPOCH);

        // Find union of missing epochs across all enabled epoch tables
        Set<Integer> allMissingEpochs = new HashSet<>();
        for (String tableName : enabledEpochTables) {
            List<Integer> tableMissingEpochs = gapDetectionService.findMissingEpochExports(tableName);
            allMissingEpochs.addAll(tableMissingEpochs);
        }

        boolean fullySynced = allMissingDates.isEmpty() && allMissingEpochs.isEmpty();

        return SyncStatus.builder()
            .genesisDate(genesis)
            .latestSyncedDate(latestSynced)
            .exportEndDate(exportEndDate)
            .bufferDays(properties.getContinuousSync().getBufferDays())
            .missingExportCount(allMissingDates.size())
            .isFullySynced(fullySynced)
            .enabledTableCount(enabledDailyTables.size())
            .missingEpochExportCount(allMissingEpochs.size())
            .enabledEpochTableCount(enabledEpochTables.size())
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
        private int missingExportCount;
        private boolean isFullySynced;
        private int enabledTableCount;
        private int missingEpochExportCount;
        private int enabledEpochTableCount;
    }
}
