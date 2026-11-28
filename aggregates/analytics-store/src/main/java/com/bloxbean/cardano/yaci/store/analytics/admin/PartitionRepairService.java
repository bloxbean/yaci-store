package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportReadinessService;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionNotReadyException;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionReadiness;
import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repairs daily partitions that earlier exports filed under the wrong day.
 *
 * Before day slot ranges started at the first slot at or after midnight, a Byron block at
 * 23:59:51 was selected by the next day's export but filed under its own day (from block_time),
 * and rows without a block time were filed under 1970-01-01. Row totals were right, but the
 * per-day export journal no longer matched the catalog.
 *
 * Each day of the range is exported again regardless of its journal record. The writer replaces
 * the day's rows (DuckLake: DELETE + INSERT in one transaction), so the result is the same as a
 * first export with the current slot ranges, and the journal records the new row count.
 *
 * A repair goes through the same readiness check as every export ({@link ExportReadinessService}):
 * a range that ends after the latest exportable day is refused, because re-exporting a day that is
 * not final would store partial data as COMPLETED. A day whose table is not ready for another
 * reason keeps its journal record and is reported as failed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PartitionRepairService {

    private final TableExporterRegistry registry;
    private final ExportStateAdminService adminService;
    private final EraService eraService;
    private final ObjectProvider<DuckLakeWriterService> duckLakeWriter;
    private final ExportReadinessService readinessService;

    /**
     * Re-export every day between startDate and endDate (inclusive) of a DAILY table, replacing
     * the exported rows. If every day succeeds, the table's date=1970-01-01 partition is deleted
     * when all of its rows have slots inside the repaired days (they were just re-inserted under
     * their real day); otherwise it is kept and reported.
     *
     * @throws PartitionNotReadyException if endDate is after the latest day that can be exported
     */
    public RepairResult repairDateRange(String tableName, LocalDate startDate, LocalDate endDate) {
        TableExporter exporter = registry.getExporter(tableName);
        if (exporter.getPartitionStrategy() != PartitionStrategy.DAILY) {
            throw new IllegalArgumentException("Repair is only supported for DAILY tables. Table "
                    + tableName + " uses " + exporter.getPartitionStrategy());
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate " + endDate + " is before startDate " + startDate);
        }

        PartitionReadiness rangeReadiness = readinessService.checkFinal(PartitionValue.ofDate(endDate));
        if (!rangeReadiness.isReady()) {
            throw new PartitionNotReadyException(rangeReadiness);
        }

        log.warn("Repairing {} partitions from {} to {}", tableName, startDate, endDate);
        int totalDays = 0;
        List<LocalDate> failedDates = new ArrayList<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            totalDays++;
            PartitionValue partition = PartitionValue.ofDate(day);
            PartitionReadiness readiness = readinessService.checkExporter(exporter, partition);
            if (!readiness.isReady()) {
                log.warn("Not repairing {} {}: {}", tableName, partition.toPathSegment(), readiness.reason());
                failedDates.add(day);
                continue;
            }
            // Without its journal record the exporter does not skip the partition
            adminService.resetExportState(tableName, partition.toPathSegment());
            if (!exporter.exportForPartition(partition)) {
                failedDates.add(day);
            }
        }

        DuckLakeWriterService.EpochZeroPartition epochZero = null;
        DuckLakeWriterService writer = duckLakeWriter.getIfAvailable();
        if (writer != null && failedDates.isEmpty()) {
            long startSlot = PartitionValue.ofDate(startDate).toSlotRange(eraService).startSlot();
            long endSlot = PartitionValue.ofDate(endDate).toSlotRange(eraService).endSlot();
            epochZero = writer.purgeEpochZeroPartition(tableName, startSlot, endSlot);
        }

        log.info("Repaired {} of {} days of {} ({} to {}); date=1970-01-01: {}", totalDays - failedDates.size(),
                totalDays, tableName, startDate, endDate, epochZero);
        return new RepairResult(tableName, startDate, endDate, totalDays - failedDates.size(), totalDays,
                failedDates, epochZero);
    }

    /**
     * @param failedDates days whose export failed (no longer COMPLETED, so the scheduler exports
     *                    them again) or whose table was not ready (journal record left as it was)
     * @param epochZero   the table's date=1970-01-01 partition, or null when it was not checked
     *                    (Parquet storage, or a day failed)
     */
    public record RepairResult(String tableName, LocalDate startDate, LocalDate endDate, int successCount,
                               int totalDays, List<LocalDate> failedDates,
                               DuckLakeWriterService.EpochZeroPartition epochZero) {
    }
}
