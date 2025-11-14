package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for continuous sync during blockchain synchronization.
 *
 * Automatically runs when analytics.enabled=true to detect and fill export gaps.
 * Exports missing dates sequentially from oldest to newest for all enabled daily tables.
 *
 * Uses TableExporterRegistry to automatically discover and export all daily tables
 * that are enabled in configuration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ContinuousSyncScheduler {

    private final GapDetectionService gapDetectionService;
    private final UniversalExportScheduler universalExportScheduler;
    private final TableExporterRegistry registry;
    private final AnalyticsStoreProperties properties;

    /**
     * Periodically check for gaps and export missing dates for all daily tables.
     *
     * Runs every N minutes (configurable via sync-check-interval-minutes).
     * Finds the union of missing dates across all enabled daily tables and exports them
     * sequentially from oldest to newest.
     */
    @Scheduled(fixedDelayString = "${yaci.store.analytics.continuous-sync.sync-check-interval-minutes:15}",
               timeUnit = TimeUnit.MINUTES)
    public void syncGaps() {
        log.debug("Running continuous sync gap detection for all daily tables");

        try {
            // Get enabled daily tables from registry
            List<String> enabledDailyTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);

            if (enabledDailyTables.isEmpty()) {
                log.debug("No daily tables enabled, skipping continuous sync");
                return;
            }

            log.debug("Checking gaps for {} enabled daily tables: {}",
                    enabledDailyTables.size(), enabledDailyTables);

            // Find missing exports across all enabled daily tables (union)
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
                return;
            }

            // Sort dates for sequential export (oldest to newest)
            List<LocalDate> sortedMissingDates = allMissingDates.stream()
                    .sorted()
                    .toList();

            log.info("Found {} unique missing dates across all daily tables, starting sequential export",
                    sortedMissingDates.size());

            // Export all daily tables for each missing date
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

            log.info("Continuous sync batch complete: {} dates successful, {} failed",
                successCount, failureCount);

        } catch (Exception e) {
            log.error("Continuous sync scheduler encountered error: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current sync status for monitoring.
     *
     * Aggregates status across all enabled daily tables.
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

        return SyncStatus.builder()
            .genesisDate(genesis)
            .latestSyncedDate(latestSynced)
            .exportEndDate(exportEndDate)
            .bufferDays(properties.getContinuousSync().getBufferDays())
            .missingExportCount(allMissingDates.size())
            .isFullySynced(allMissingDates.isEmpty())
            .enabledTableCount(enabledDailyTables.size())
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
    }
}
