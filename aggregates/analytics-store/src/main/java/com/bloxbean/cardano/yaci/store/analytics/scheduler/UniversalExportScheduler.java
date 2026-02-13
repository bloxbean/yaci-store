package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Universal scheduler for exporting all registered tables.
 *
 * This scheduler replaces table-specific schedulers with a unified approach:
 * - Automatically discovers and exports all enabled tables
 * - Supports multiple partition strategies (DAILY, EPOCH)
 * - Uses finalization buffer to ensure immutable data
 * - Provides admin methods for manual exports and backfills
 *
 * Scheduled Tasks:
 * 1. exportDailyTables() - Runs at midnight (default), exports all daily tables
 * 2. exportEpochTables() - Runs at 1 AM (default), exports all epoch tables
 *
 * Manual Operations:
 * - exportTable() - Export specific table for specific partition
 * - exportDateRange() - Backfill date range for daily table
 * - exportEpochRange() - Backfill epoch range for epoch table
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class UniversalExportScheduler {

    private final TableExporterRegistry registry;
    private final AnalyticsStoreProperties properties;
    private final BlockStorageReader blockStorageReader;

    @Getter
    private final AtomicBoolean isDailyExporting = new AtomicBoolean(false);
    @Getter
    private final AtomicBoolean isEpochExporting = new AtomicBoolean(false);

    @Getter
    private volatile Instant lastDailyExportStart;
    @Getter
    private volatile Instant lastDailyExportEnd;
    @Getter
    private volatile Instant lastEpochExportStart;
    @Getter
    private volatile Instant lastEpochExportEnd;

    /**
     * Export all daily tables for finalized date.
     *
     * Runs at midnight (default) and exports data from N days ago,
     * where N = finalizationLagDays (default: 2).
     *
     * This ensures:
     * - Data is past Cardano's 2160 block security parameter (~12 hours)
     * - Additional buffer for potential chain reorganizations
     * - Exported data is immutable and safe for analytics
     *
     * Example: If today is 2024-01-17 and finalizationLagDays=2:
     * - Exports data for 2024-01-15
     */
    @Scheduled(cron = "${yaci.store.analytics.daily-export-cron:0 0 0 * * *}")
    public void exportDailyTables() {
        if (!isDailyExporting.compareAndSet(false, true)) {
            log.warn("Daily export is already in progress, skipping this run");
            return;
        }

        lastDailyExportStart = Instant.now();
        try {
            // Calculate finalized date (N days ago)
            LocalDate exportDate = LocalDate.now().minusDays(properties.getFinalizationLagDays());

            log.info("Starting daily table exports for date: {} (finalization lag: {} days)",
                    exportDate, properties.getFinalizationLagDays());

            // Get enabled daily tables
            List<String> enabledDailyTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);

            if (enabledDailyTables.isEmpty()) {
                log.info("No daily tables enabled, skipping export");
                return;
            }

            log.info("Exporting {} daily tables: {}", enabledDailyTables.size(), enabledDailyTables);

            int successCount = 0;
            int failureCount = 0;

            for (String tableName : enabledDailyTables) {
                try {
                    TableExporter exporter = registry.getExporter(tableName);
                    boolean success = exporter.exportForPartition(PartitionValue.ofDate(exportDate));

                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to export table {}: {}", tableName, e.getMessage(), e);
                    failureCount++;
                }
            }

            log.info("Daily export completed: {} successful, {} failed", successCount, failureCount);
        } finally {
            lastDailyExportEnd = Instant.now();
            isDailyExporting.set(false);
        }
    }

    /**
     * Export all epoch tables for the previous completed epoch.
     *
     * Runs at 1 AM (default) and exports data for the most recently
     * completed epoch (currentEpoch - 1).
     *
     * This ensures:
     * - Epoch is fully completed (all 432,000 slots)
     * - All epoch-specific data (rewards, stake snapshots) is available
     * - No partial epoch exports
     *
     * Example: If current epoch is 451:
     * - Exports data for epoch 450
     */
    @Scheduled(cron = "${yaci.store.analytics.epoch-export-cron:0 0 1 * * *}")
    public void exportEpochTables() {
        if (!isEpochExporting.compareAndSet(false, true)) {
            log.warn("Epoch export is already in progress, skipping this run");
            return;
        }

        lastEpochExportStart = Instant.now();
        try {
            // Get current epoch from latest block and export previous (completed) epoch
            Optional<Block> latestBlock = blockStorageReader.findRecentBlock();
            if (latestBlock.isEmpty()) {
                log.warn("No blocks found, cannot determine current epoch. Skipping epoch export.");
                return;
            }

            int currentEpoch = latestBlock.get().getEpochNumber();
            int exportEpoch = currentEpoch - 1;

            log.info("Starting epoch table exports for epoch: {} (current epoch: {})",
                    exportEpoch, currentEpoch);

            // Get enabled epoch tables
            List<String> enabledEpochTables = registry.getEnabledTablesByStrategy(PartitionStrategy.EPOCH);

            if (enabledEpochTables.isEmpty()) {
                log.info("No epoch tables enabled, skipping export");
                return;
            }

            log.info("Exporting {} epoch tables: {}", enabledEpochTables.size(), enabledEpochTables);

            int successCount = 0;
            int failureCount = 0;

            for (String tableName : enabledEpochTables) {
                try {
                    TableExporter exporter = registry.getExporter(tableName);
                    boolean success = exporter.exportForPartition(PartitionValue.ofEpoch(exportEpoch));

                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to export table {}: {}", tableName, e.getMessage(), e);
                    failureCount++;
                }
            }

            log.info("Epoch export completed: {} successful, {} failed", successCount, failureCount);
        } finally {
            lastEpochExportEnd = Instant.now();
            isEpochExporting.set(false);
        }
    }

    /**
     * Manual export for specific table and partition.
     *
     * Can be called from admin endpoints or CLI commands.
     *
     * @param tableName Table to export
     * @param partition Partition to export (date or epoch)
     * @return true if export succeeded
     */
    public boolean exportTable(String tableName, PartitionValue partition) {
        log.info("Manual export triggered: {} for partition {}", tableName, partition.toPathSegment());

        TableExporter exporter = registry.getExporter(tableName);
        return exporter.exportForPartition(partition);
    }

    /**
     * Export date range for a daily table (backfill).
     *
     * @param tableName Table to export
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of successful exports
     */
    public int exportDateRange(String tableName, LocalDate startDate, LocalDate endDate) {
        log.info("Exporting date range for {}: {} to {}", tableName, startDate, endDate);

        TableExporter exporter = registry.getExporter(tableName);

        if (exporter.getPartitionStrategy() != PartitionStrategy.DAILY) {
            throw new IllegalArgumentException(
                    "Date range export only supported for DAILY tables. " +
                    "Table " + tableName + " uses " + exporter.getPartitionStrategy());
        }

        int successCount = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            boolean success = exporter.exportForPartition(PartitionValue.ofDate(current));
            if (success) {
                successCount++;
            }
            current = current.plusDays(1);
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        log.info("Completed date range export for {}: {} of {} days successful",
                tableName, successCount, totalDays);

        return successCount;
    }

    /**
     * Export epoch range for an epoch table (backfill).
     *
     * @param tableName Table to export
     * @param startEpoch Start epoch (inclusive)
     * @param endEpoch End epoch (inclusive)
     * @return Number of successful exports
     */
    public int exportEpochRange(String tableName, int startEpoch, int endEpoch) {
        log.info("Exporting epoch range for {}: {} to {}", tableName, startEpoch, endEpoch);

        TableExporter exporter = registry.getExporter(tableName);

        if (exporter.getPartitionStrategy() != PartitionStrategy.EPOCH) {
            throw new IllegalArgumentException(
                    "Epoch range export only supported for EPOCH tables. " +
                    "Table " + tableName + " uses " + exporter.getPartitionStrategy());
        }

        int successCount = 0;

        for (int epoch = startEpoch; epoch <= endEpoch; epoch++) {
            boolean success = exporter.exportForPartition(PartitionValue.ofEpoch(epoch));
            if (success) {
                successCount++;
            }
        }

        int totalEpochs = endEpoch - startEpoch + 1;
        log.info("Completed epoch range export for {}: {} of {} epochs successful",
                tableName, successCount, totalEpochs);

        return successCount;
    }

    /**
     * Export all enabled daily tables for a specific date.
     *
     * @param date Date to export
     * @return Number of successful exports
     */
    public int exportAllDailyTables(LocalDate date) {
        log.info("Exporting all daily tables for date: {}", date);

        List<String> enabledDailyTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);
        int successCount = 0;

        for (String tableName : enabledDailyTables) {
            try {
                TableExporter exporter = registry.getExporter(tableName);
                boolean success = exporter.exportForPartition(PartitionValue.ofDate(date));
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to export table {}: {}", tableName, e.getMessage(), e);
            }
        }

        log.info("Exported {} of {} daily tables for {}", successCount, enabledDailyTables.size(), date);
        return successCount;
    }

    /**
     * Export all enabled epoch tables for a specific epoch.
     *
     * @param epoch Epoch to export
     * @return Number of successful exports
     */
    public int exportAllEpochTables(int epoch) {
        log.info("Exporting all epoch tables for epoch: {}", epoch);

        List<String> enabledEpochTables = registry.getEnabledTablesByStrategy(PartitionStrategy.EPOCH);
        int successCount = 0;

        for (String tableName : enabledEpochTables) {
            try {
                TableExporter exporter = registry.getExporter(tableName);
                boolean success = exporter.exportForPartition(PartitionValue.ofEpoch(epoch));
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to export table {}: {}", tableName, e.getMessage(), e);
            }
        }

        log.info("Exported {} of {} epoch tables for epoch {}", successCount, enabledEpochTables.size(), epoch);
        return successCount;
    }
}
