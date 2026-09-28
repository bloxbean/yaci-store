package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for exporting all registered analytics tables.
 *
 * Provides a unified export API for all enabled tables:
 * - Automatically discovers and exports all enabled tables
 * - Supports multiple partition strategies (DAILY, EPOCH)
 * - Immutability of the exported data is ensured by the caller's date/epoch choice
 *   (the scheduler stays {@code continuous-sync.buffer-days} behind the tip)
 *
 * Called by:
 * - {@link ContinuousSyncScheduler} for automated gap-based exports
 * - {@link com.bloxbean.cardano.yaci.store.analytics.admin.AnalyticsAdminController} for manual/admin exports
 *
 * Export Methods:
 * - exportAllDailyTables() - Export all daily tables for a specific date
 * - exportAllEpochTables() - Export all epoch tables for a specific epoch
 * - exportTable() - Export specific table for specific partition
 * - exportDateRange() - Backfill date range for daily table
 * - exportEpochRange() - Backfill epoch range for epoch table
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class UniversalExportService {

    private final TableExporterRegistry registry;

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
