package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for exporting all registered analytics tables.
 *
 * Provides a unified export API for all enabled tables:
 * - Automatically discovers and exports all enabled tables
 * - Supports multiple partition strategies (DAILY, EPOCH)
 * - Exports a partition only when {@link ExportReadinessService} says it is ready, whoever the
 *   caller is, so that no partition is stored as COMPLETED before its data is final
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
    private final EraService eraService;
    private final ExportReadinessService readinessService;

    /** Outcome of the export of one partition. */
    public enum Outcome {
        /** Exported, or already exported (COMPLETED with its data present). */
        EXPORTED,
        /** Not ready (see {@link PartitionReadiness}); nothing was exported or marked. */
        NOT_READY,
        /** The export ran and failed; the partition is FAILED and retried by the scheduler. */
        FAILED
    }

    /**
     * @param readiness the readiness check result; for {@link Outcome#NOT_READY} it says why
     */
    public record PartitionExportResult(String tableName, String partition, Outcome outcome,
                                        PartitionReadiness readiness) {
    }

    /**
     * @param notReady partitions skipped because they were not ready
     * @param failed   partitions whose export failed
     */
    public record RangeExportResult(String tableName, int total, int successCount, List<String> notReady,
                                    List<String> failed) {
    }

    /**
     * Manual export for specific table and partition.
     *
     * Can be called from admin endpoints or CLI commands.
     *
     * @param tableName Table to export
     * @param partition Partition to export (date or epoch)
     * @return the outcome
     * @throws IllegalArgumentException for an unknown table or a partition type that does not
     *                                  match the table
     */
    public PartitionExportResult exportTable(String tableName, PartitionValue partition) {
        log.info("Manual export triggered: {} for partition {}", tableName, partition.toPathSegment());

        TableExporter exporter = registry.getExporter(tableName);
        return export(exporter, partition, readinessService.check(exporter, partition));
    }

    /**
     * Export date range for a daily table (backfill). Days that are not ready are skipped and
     * reported.
     *
     * @param tableName Table to export
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return per-range counts
     */
    public RangeExportResult exportDateRange(String tableName, LocalDate startDate, LocalDate endDate) {
        log.info("Exporting date range for {}: {} to {}", tableName, startDate, endDate);

        TableExporter exporter = registry.getExporter(tableName);

        if (exporter.getPartitionStrategy() != PartitionStrategy.DAILY) {
            throw new IllegalArgumentException(
                    "Date range export only supported for DAILY tables. " +
                    "Table " + tableName + " uses " + exporter.getPartitionStrategy());
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate " + endDate + " is before startDate " + startDate);
        }

        RangeCollector collector = new RangeCollector(tableName);
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            PartitionValue partition = PartitionValue.ofDate(day);
            collector.add(export(exporter, partition, readinessService.check(exporter, partition)));
        }

        RangeExportResult result = collector.result();
        log.info("Completed date range export for {}: {} of {} days successful, {} not ready, {} failed",
                tableName, result.successCount(), result.total(), result.notReady().size(), result.failed().size());
        return result;
    }

    /**
     * Export epoch range for an epoch table (backfill). Epochs that are not ready are skipped and
     * reported.
     *
     * @param tableName Table to export
     * @param startEpoch Start epoch (inclusive)
     * @param endEpoch End epoch (inclusive)
     * @return per-range counts
     */
    public RangeExportResult exportEpochRange(String tableName, int startEpoch, int endEpoch) {
        log.info("Exporting epoch range for {}: {} to {}", tableName, startEpoch, endEpoch);

        TableExporter exporter = registry.getExporter(tableName);

        if (exporter.getPartitionStrategy() != PartitionStrategy.EPOCH) {
            throw new IllegalArgumentException(
                    "Epoch range export only supported for EPOCH tables. " +
                    "Table " + tableName + " uses " + exporter.getPartitionStrategy());
        }
        if (startEpoch < 0 || endEpoch < startEpoch) {
            throw new IllegalArgumentException("Invalid epoch range " + startEpoch + " to " + endEpoch
                    + ": epochs must not be negative and endEpoch must not be before startEpoch");
        }

        RangeCollector collector = new RangeCollector(tableName);
        // A long counter: "epoch <= endEpoch" never ends for endEpoch = Integer.MAX_VALUE
        for (long epoch = startEpoch; epoch <= endEpoch; epoch++) {
            PartitionValue partition = PartitionValue.ofEpoch((int) epoch);
            collector.add(export(exporter, partition, readinessService.check(exporter, partition)));
        }

        RangeExportResult result = collector.result();
        log.info("Completed epoch range export for {}: {} of {} epochs successful, {} not ready, {} failed",
                tableName, result.successCount(), result.total(), result.notReady().size(), result.failed().size());
        return result;
    }

    /**
     * Export all enabled daily tables for a specific date. Nothing is exported when the day is not
     * final yet; tables whose own prerequisites are not met are skipped.
     *
     * @param date Date to export
     * @return Number of successful exports
     */
    public int exportAllDailyTables(LocalDate date) {
        log.info("Exporting all daily tables for date: {}", date);

        PartitionValue partition = PartitionValue.ofDate(date);
        PartitionReadiness dayReadiness = readinessService.checkFinal(partition);
        if (!dayReadiness.isReady()) {
            log.info("Not exporting daily tables for {}: {}", date, dayReadiness.reason());
            return 0;
        }

        List<String> enabledDailyTables = registry.getExportableTablesByStrategy(PartitionStrategy.DAILY);
        int successCount = 0;

        for (String tableName : enabledDailyTables) {
            try {
                TableExporter exporter = registry.getExporter(tableName);
                PartitionExportResult result = export(exporter, partition,
                        readinessService.checkExporter(exporter, partition));
                if (result.outcome() == Outcome.EXPORTED) {
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
     * Export all enabled epoch tables for a specific epoch. Tables that are not ready for the epoch
     * are skipped.
     *
     * @param epoch Epoch to export
     * @return Number of successful exports
     */
    public int exportAllEpochTables(int epoch) {
        log.info("Exporting all epoch tables for epoch: {}", epoch);

        PartitionValue partition = PartitionValue.ofEpoch(epoch);
        PartitionReadiness epochReadiness = readinessService.checkFinal(partition);
        if (!epochReadiness.isReady()) {
            log.info("Not exporting epoch tables for epoch {}: {}", epoch, epochReadiness.reason());
            return 0;
        }

        List<String> enabledEpochTables = registry.getExportableTablesByStrategy(PartitionStrategy.EPOCH);
        int successCount = 0;

        boolean byronEpoch = eraService.getFirstNonByronEpoch().map(first -> epoch < first).orElse(true);
        for (String tableName : enabledEpochTables) {
            try {
                TableExporter exporter = registry.getExporter(tableName);
                if (byronEpoch && !exporter.includesByronEpochs()) {
                    continue;
                }
                PartitionExportResult result = export(exporter, partition,
                        readinessService.checkExporter(exporter, partition));
                if (result.outcome() == Outcome.EXPORTED) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to export table {}: {}", tableName, e.getMessage(), e);
            }
        }

        log.info("Exported {} of {} epoch tables for epoch {}", successCount, enabledEpochTables.size(), epoch);
        return successCount;
    }

    private PartitionExportResult export(TableExporter exporter, PartitionValue partition,
                                         PartitionReadiness readiness) {
        String tableName = exporter.getTableName();
        String key = partition.toPathSegment();
        if (!readiness.isReady()) {
            log.debug("Not exporting {} {}: {}", tableName, key, readiness.reason());
            return new PartitionExportResult(tableName, key, Outcome.NOT_READY, readiness);
        }
        boolean success;
        try {
            success = exporter.exportForPartition(partition);
        } catch (Exception e) {
            log.error("Failed to export {} {}: {}", tableName, key, e.getMessage(), e);
            success = false;
        }
        return new PartitionExportResult(tableName, key, success ? Outcome.EXPORTED : Outcome.FAILED, readiness);
    }

    private static final class RangeCollector {
        private final String tableName;
        private int total;
        private int success;
        private final List<String> notReady = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();

        RangeCollector(String tableName) {
            this.tableName = tableName;
        }

        void add(PartitionExportResult result) {
            total++;
            switch (result.outcome()) {
                case EXPORTED -> success++;
                case NOT_READY -> notReady.add(result.partition());
                case FAILED -> failed.add(result.partition());
            }
        }

        RangeExportResult result() {
            return new RangeExportResult(tableName, total, success, List.copyOf(notReady), List.copyOf(failed));
        }
    }
}
