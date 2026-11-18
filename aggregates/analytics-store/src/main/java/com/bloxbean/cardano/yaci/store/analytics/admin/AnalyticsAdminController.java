package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ContinuousSyncScheduler;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportReadinessService;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionNotReadyException;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.PartitionReadiness;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.UniversalExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Admin endpoints for analytics export management.
 *
 * Disabled by default - enable via yaci.store.analytics.admin.enabled=true
 *
 * Provides manual control over exports, state management, and monitoring:
 * - Export all tables or specific tables
 * - Export single dates or date ranges
 * - Export specific epochs
 * - Monitor sync status
 * - Reset export state for re-exports
 * - Repair daily partitions filed under the wrong day by earlier exports
 *
 * Exports replace the partition they write (DuckLake: DELETE + INSERT in one transaction), so
 * exporting a partition again is idempotent.
 *
 * Every export and repair goes through the same readiness check as the scheduler
 * ({@link ExportReadinessService}): a day after the latest exportable day, an epoch that has not
 * started, or a partition whose table is not ready is refused with 409 and nothing is marked.
 * Invalid input gives 400 and an unknown table 404 ({@link AnalyticsAdminExceptionHandler}).
 * Range sizes are capped by {@code admin.max-range-days} / {@code admin.max-range-epochs}, and one
 * manual export or repair runs at a time.
 *
 * The API has no authentication. Do not expose it outside a trusted network.
 */
@RestController
@RequestMapping("/api/v1/analytics/admin")
@Tag(name = "Analytics Admin API",
        description = "Operations API for the analytics export pipeline: manual exports, export state and sync status")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.admin", name = "enabled", havingValue = "true")
public class AnalyticsAdminController {

    private final UniversalExportService universalExportService;
    private final ContinuousSyncScheduler continuousSyncScheduler;
    private final TableExporterRegistry registry;
    private final ExportStateAdminService adminService;
    private final PartitionRepairService repairService;
    private final ExportReadinessService readinessService;
    private final AnalyticsStoreProperties properties;

    /** One manual export or repair at a time: each runs synchronously on the request thread. */
    private final AtomicBoolean manualExportRunning = new AtomicBoolean();

    /**
     * List all registered table exporters.
     *
     * @return List of table information
     */
    @Operation(summary = "List table exporters",
            description = "All registered analytics table exporters with their partition strategy (DAILY / EPOCH / NONE) "
                    + "and whether the exporter is enabled.")
    @GetMapping("/tables")
    public ResponseEntity<List<TableInfo>> listTables() {
        List<TableInfo> tables = registry.getAllTables().stream()
                .map(tableName -> {
                    var exporter = registry.getExporter(tableName);
                    return new TableInfo(
                            tableName,
                            exporter.getPartitionStrategy().name(),
                            registry.isEnabled(tableName)
                    );
                })
                .sorted((a, b) -> a.tableName.compareTo(b.tableName))
                .toList();

        return ResponseEntity.ok(tables);
    }

    /**
     * Export all enabled daily tables for a specific date.
     *
     * @param date Date in yyyy-MM-dd format
     * @return Export result
     */
    @Operation(summary = "Export all daily tables for one day",
            description = "Runs the export of every enabled DAILY-partitioned table for the given date (yyyy-MM-dd). "
                    + "Partitions that are already exported are skipped; reset the partition state first to re-export. "
                    + "A re-export replaces the partition's rows instead of appending to them. "
                    + "409 if the day is after the latest exportable day (status.exportEndDate), 400 for an invalid "
                    + "date or a day before genesis.")
    @PostMapping("/export/date/{date}")
    public ResponseEntity<ExportResult> exportDate(@PathVariable String date) {
        LocalDate exportDate = parseDate("date", date);
        requireNotBeforeGenesis(exportDate);
        requireFinal(PartitionValue.ofDate(exportDate));
        log.info("Manual export triggered for all daily tables for date: {}", exportDate);

        int successCount = exclusive(() -> universalExportService.exportAllDailyTables(exportDate));
        List<String> enabledTables = registry.getExportableTablesByStrategy(PartitionStrategy.DAILY);

        return ResponseEntity.ok(new ExportResult(
                "all_daily_tables",
                date,
                successCount + " of " + enabledTables.size() + " tables exported successfully"
        ));
    }

    /**
     * Export specific table for a specific date.
     *
     * @param tableName Table name
     * @param date Date in yyyy-MM-dd format
     * @return Export result
     */
    @Operation(summary = "Export one table for one day",
            description = "Runs the export of a single DAILY-partitioned table for the given date (yyyy-MM-dd). "
                    + "Skipped if that partition is already exported. 404 for an unknown table, 400 for an invalid date "
                    + "or a non-DAILY table, 409 if the day is not final or the table is not ready for it, 500 if "
                    + "the export failed.")
    @PostMapping("/export/table/{tableName}/date/{date}")
    public ResponseEntity<ExportResult> exportTableDate(
            @PathVariable String tableName,
            @PathVariable String date) {

        requireExporter(tableName, PartitionStrategy.DAILY);
        LocalDate exportDate = parseDate("date", date);
        requireNotBeforeGenesis(exportDate);
        log.info("Manual export triggered for table {} for date: {}", tableName, exportDate);

        return toResponse(exclusive(() ->
                universalExportService.exportTable(tableName, PartitionValue.ofDate(exportDate))));
    }

    /**
     * Export specific table for a specific epoch.
     *
     * @param tableName Table name
     * @param epoch Epoch number
     * @return Export result
     */
    @Operation(summary = "Export one table for one epoch",
            description = "Runs the export of a single EPOCH-partitioned table for the given epoch. "
                    + "Skipped if that partition is already exported. 404 for an unknown table, 400 for a negative epoch "
                    + "or a non-EPOCH table, 409 if the epoch has not started or the table is not ready for it (for "
                    + "example its AdaPot job has not finished), 500 if the export failed.")
    @PostMapping("/export/table/{tableName}/epoch/{epoch}")
    public ResponseEntity<ExportResult> exportTableEpoch(
            @PathVariable String tableName,
            @PathVariable int epoch) {

        requireExporter(tableName, PartitionStrategy.EPOCH);
        requireNonNegativeEpoch("epoch", epoch);
        log.info("Manual export triggered for table {} for epoch: {}", tableName, epoch);

        return toResponse(exclusive(() ->
                universalExportService.exportTable(tableName, PartitionValue.ofEpoch(epoch))));
    }

    /**
     * Export specific table for a date range.
     *
     * @param tableName Table name
     * @param startDate Start date (inclusive, yyyy-MM-dd format)
     * @param endDate End date (inclusive, yyyy-MM-dd format)
     * @return Backfill result
     */
    @Operation(summary = "Backfill one table over a date range",
            description = "Exports every day between startDate and endDate (inclusive, yyyy-MM-dd) for a DAILY-partitioned "
                    + "table, skipping partitions that are already exported. Returns the number of successful days and "
                    + "the days that were not ready or failed. 400 for a reversed range or one longer than "
                    + "admin.max-range-days, 409 if endDate is after the latest exportable day.")
    @PostMapping("/export/table/{tableName}/range")
    public ResponseEntity<BackfillResult> exportTableRange(
            @PathVariable String tableName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        requireExporter(tableName, PartitionStrategy.DAILY);
        LocalDate start = parseDate("startDate", startDate);
        LocalDate end = parseDate("endDate", endDate);
        requireDateRange(start, end);
        requireFinal(PartitionValue.ofDate(end));

        log.info("Manual range export triggered for table {}: {} to {}", tableName, start, end);
        UniversalExportService.RangeExportResult result =
                exclusive(() -> universalExportService.exportDateRange(tableName, start, end));

        return ResponseEntity.ok(new BackfillResult(
                tableName,
                start,
                end,
                result.successCount(),
                result.total(),
                result.notReady(),
                result.failed()
        ));
    }

    /**
     * Export specific table for an epoch range.
     *
     * @param tableName Table name
     * @param startEpoch Start epoch (inclusive)
     * @param endEpoch End epoch (inclusive)
     * @return Backfill result
     */
    @Operation(summary = "Backfill one table over an epoch range",
            description = "Exports every epoch between startEpoch and endEpoch (inclusive) for an EPOCH-partitioned "
                    + "table, skipping partitions that are already exported. Returns the epochs that were not ready or "
                    + "failed. 400 for a negative or reversed range or one longer than admin.max-range-epochs, 409 if "
                    + "endEpoch has not started.")
    @PostMapping("/export/table/{tableName}/epoch-range")
    public ResponseEntity<EpochBackfillResult> exportTableEpochRange(
            @PathVariable String tableName,
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {

        requireExporter(tableName, PartitionStrategy.EPOCH);
        requireEpochRange(startEpoch, endEpoch);
        requireFinal(PartitionValue.ofEpoch(endEpoch));

        log.info("Manual epoch range export triggered for table {}: {} to {}", tableName, startEpoch, endEpoch);
        UniversalExportService.RangeExportResult result =
                exclusive(() -> universalExportService.exportEpochRange(tableName, startEpoch, endEpoch));

        return ResponseEntity.ok(new EpochBackfillResult(
                tableName,
                startEpoch,
                endEpoch,
                result.successCount(),
                result.total(),
                result.notReady(),
                result.failed()
        ));
    }

    /**
     * Repair a daily table over a date range.
     *
     * @param tableName Table name
     * @param startDate Start date (inclusive, yyyy-MM-dd format)
     * @param endDate End date (inclusive, yyyy-MM-dd format)
     * @return Repair result
     */
    @Operation(summary = "Repair one daily table over a date range",
            description = "Re-exports every day between startDate and endDate (inclusive, yyyy-MM-dd) of a DAILY-partitioned "
                    + "table, including days already exported, replacing each day's rows (DuckLake: DELETE + INSERT in "
                    + "one transaction) and refreshing the export journal's row count. Use it for catalogs exported "
                    + "before day slot ranges started at the first slot at or after midnight UTC, where the journal's "
                    + "per-day row counts differ from the catalog although the table totals match:\n"
                    + "- Byron days: the block in the slot starting at 23:59:51 was selected by the next day's export "
                    + "but filed under its own day. The fix moves rows between adjacent days, so start the range one "
                    + "day before and end it one day after the affected days (on mainnet, the Byron era is "
                    + "2017-09-23 to 2020-07-29).\n"
                    + "- Rows without a block time were filed under date=1970-01-01. Include the days holding their "
                    + "slots: once every day exported successfully, the 1970-01-01 partition is deleted if all its rows "
                    + "have slots inside the repaired days; otherwise it is kept and its slot range is returned in "
                    + "epochZero so the range can be widened.\n"
                    + "Re-running a repair is safe. Each day is one export, so a long range takes as long as the "
                    + "original backfill. Days that fail are listed in failedDates and left for the scheduler to retry; "
                    + "days whose table is not ready are listed there too and keep their export state.\n"
                    + "409 if endDate is after the latest exportable day (repairing a day that is not final would "
                    + "store partial data), 400 for a reversed range or one longer than admin.max-range-days.")
    @PostMapping("/repair/table/{tableName}/range")
    public ResponseEntity<PartitionRepairService.RepairResult> repairTableRange(
            @PathVariable String tableName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        requireExporter(tableName, PartitionStrategy.DAILY);
        LocalDate start = parseDate("startDate", startDate);
        LocalDate end = parseDate("endDate", endDate);
        requireDateRange(start, end);

        log.warn("Manual repair triggered for table {}: {} to {}", tableName, start, end);
        return ResponseEntity.ok(exclusive(() -> repairService.repairDateRange(tableName, start, end)));
    }

    /**
     * Get current sync status.
     *
     * @return SyncStatus with current sync information
     */
    @Operation(summary = "Export pipeline status",
            description = "Continuous-sync status: genesis and latest synced dates, the export end date (latest synced "
                    + "date minus the buffer), buffer days, number of missing daily/epoch exports, whether exports are "
                    + "deferred until the node reaches the chain tip, and whether everything is fully exported.")
    @GetMapping("/status")
    public ResponseEntity<ContinuousSyncScheduler.SyncStatus> getStatus() {
        return ResponseEntity.ok(continuousSyncScheduler.getSyncStatus());
    }

    /**
     * Get export statistics for a specific table.
     *
     * @param tableName Table name
     * @return ExportStatistics with aggregate information
     */
    @Operation(summary = "Export statistics of one table",
            description = "Counts of completed / failed / in-progress partitions and rows for the given table.")
    @GetMapping("/statistics/{tableName}")
    public ResponseEntity<ExportStateAdminService.ExportStatistics> getStatistics(
            @PathVariable String tableName) {
        return ResponseEntity.ok(adminService.getStatistics(tableName));
    }

    /**
     * Reset export state for a specific table and partition.
     *
     * Allows re-export of a single partition.
     *
     * @param tableName Table name
     * @param partitionValue Partition value (e.g., "2024-01-15", "date=2024-01-15", "450" or "epoch=450")
     * @return Success message
     */
    @Operation(summary = "Reset the export state of one partition",
            description = "Deletes the export-state record of one partition so that the scheduler or a manual export "
                    + "runs it again. The partition may be written '2024-01-15', 'date=2024-01-15', '450' or "
                    + "'epoch=450'. Exported data stays until that re-export, which replaces the partition's rows "
                    + "(DuckLake: DELETE + INSERT in one transaction) rather than appending to them. "
                    + "404 for an unknown table or when the partition has no export state, 400 for a partition "
                    + "that does not match the table's partitioning.")
    @DeleteMapping("/state/{tableName}/{partitionValue}")
    public ResponseEntity<String> resetState(
            @PathVariable String tableName,
            @PathVariable String partitionValue) {

        TableExporter exporter = requireExporter(tableName);
        PartitionValue partition = PartitionValue.parse(partitionValue);
        PartitionStrategy partitionStrategy = partition instanceof PartitionValue.DatePartition
                ? PartitionStrategy.DAILY : PartitionStrategy.EPOCH;
        if (exporter.getPartitionStrategy() != partitionStrategy) {
            throw AdminApiException.badRequest("Table " + tableName + " is partitioned "
                    + exporter.getPartitionStrategy() + "; " + partition.toPathSegment() + " is not one of its partitions");
        }

        log.warn("Resetting export state for table {} partition: {}", tableName, partition.toPathSegment());
        if (!adminService.resetExportState(tableName, partition.toPathSegment())) {
            throw AdminApiException.notFound("No export state for " + tableName + " " + partition.toPathSegment());
        }
        return ResponseEntity.ok("State reset for " + tableName + " " + partition.toPathSegment()
                + ". The exported data is untouched until the partition is exported again");
    }

    /**
     * Reset export state for a table's date range.
     *
     * Allows bulk re-export of multiple dates.
     *
     * @param tableName Table name
     * @param startDate Start date (inclusive, yyyy-MM-dd format)
     * @param endDate End date (inclusive, yyyy-MM-dd format)
     * @return Number of states reset
     */
    @Operation(summary = "Reset the export state of a date range",
            description = "Deletes the export-state records of every day between startDate and endDate (inclusive, "
                    + "yyyy-MM-dd) for the given DAILY table. Same caveats as the single-partition reset. "
                    + "404 for an unknown table or when no day in the range has export state, 400 for an invalid "
                    + "or reversed range or a non-DAILY table.")
    @DeleteMapping("/state/{tableName}/range")
    public ResponseEntity<String> resetStateRange(
            @PathVariable String tableName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        requireExporter(tableName, PartitionStrategy.DAILY);
        LocalDate start = parseDate("startDate", startDate);
        LocalDate end = parseDate("endDate", endDate);
        if (end.isBefore(start)) {
            throw AdminApiException.badRequest("endDate " + end + " is before startDate " + start);
        }

        log.warn("Resetting export state range for table {}: {} to {}", tableName, start, end);
        int count = adminService.resetDateRange(tableName, start, end);
        if (count == 0) {
            throw AdminApiException.notFound("No export state for " + tableName + " between " + start + " and " + end);
        }
        return ResponseEntity.ok(String.format("Reset %d partition states", count));
    }

    /**
     * Reset the export state of an epoch range of an EPOCH table, so those epochs are exported again.
     */
    @Operation(summary = "Reset the export state of an epoch range",
            description = "Deletes the export-state records of every epoch from startEpoch to endEpoch (inclusive) "
                    + "for the given EPOCH table. Same caveats as the single-partition reset. The range is not capped: "
                    + "a reset is cheap, and the scheduler re-exports the epochs at its own pace. "
                    + "404 for an unknown table or when no epoch in the range has export state, 400 for a negative "
                    + "or reversed range or a non-EPOCH table.")
    @DeleteMapping("/state/{tableName}/epoch-range")
    public ResponseEntity<String> resetStateEpochRange(
            @PathVariable String tableName,
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {

        requireExporter(tableName, PartitionStrategy.EPOCH);
        requireNonNegativeEpoch("startEpoch", startEpoch);
        requireNonNegativeEpoch("endEpoch", endEpoch);
        if (endEpoch < startEpoch) {
            throw AdminApiException.badRequest("endEpoch " + endEpoch + " is before startEpoch " + startEpoch);
        }

        log.warn("Resetting export state epoch range for table {}: {} to {}", tableName, startEpoch, endEpoch);
        int count = adminService.resetEpochRange(tableName, startEpoch, endEpoch);
        if (count == 0) {
            throw AdminApiException.notFound("No export state for " + tableName + " between epoch " + startEpoch
                    + " and " + endEpoch);
        }
        return ResponseEntity.ok(String.format("Reset %d partition states", count));
    }

    /**
     * Trigger immediate gap sync check.
     *
     * Bypasses the scheduled interval and runs gap detection immediately.
     *
     * @return Success message
     */
    @Operation(summary = "Trigger a sync check now",
            description = "Runs the continuous-sync check immediately instead of waiting for the next scheduled run: "
                    + "finds missing daily/epoch partitions and exports them.")
    @PostMapping("/sync/trigger")
    public ResponseEntity<String> triggerSync() {
        log.info("Manual sync triggered via API");
        continuousSyncScheduler.syncGaps();
        return ResponseEntity.ok("Sync triggered");
    }

    // Validation

    private TableExporter requireExporter(String tableName) {
        if (!registry.hasExporter(tableName)) {
            throw AdminApiException.notFound("Unknown table: " + tableName
                    + ". GET /api/v1/analytics/admin/tables lists the tables");
        }
        return registry.getExporter(tableName);
    }

    private TableExporter requireExporter(String tableName, PartitionStrategy strategy) {
        TableExporter exporter = requireExporter(tableName);
        if (exporter.getPartitionStrategy() != strategy) {
            throw AdminApiException.badRequest("Table " + tableName + " is partitioned "
                    + exporter.getPartitionStrategy() + "; this endpoint needs a " + strategy + " table");
        }
        return exporter;
    }

    private static LocalDate parseDate(String name, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw AdminApiException.badRequest("Invalid " + name + " '" + value + "': expected yyyy-MM-dd");
        }
    }

    private void requireNotBeforeGenesis(LocalDate date) {
        LocalDate genesis = readinessService.getGenesisDate();
        if (date.isBefore(genesis)) {
            throw AdminApiException.badRequest(date + " is before the network's genesis day " + genesis);
        }
    }

    private void requireDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw AdminApiException.badRequest("endDate " + end + " is before startDate " + start);
        }
        requireNotBeforeGenesis(start);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int max = properties.getAdmin().getMaxRangeDays();
        if (days > max) {
            throw AdminApiException.badRequest("The range covers " + days + " days; at most " + max
                    + " are allowed per request (yaci.store.analytics.admin.max-range-days)");
        }
    }

    private static void requireNonNegativeEpoch(String name, int epoch) {
        if (epoch < 0) {
            throw AdminApiException.badRequest(name + " must not be negative: " + epoch);
        }
    }

    private void requireEpochRange(int startEpoch, int endEpoch) {
        requireNonNegativeEpoch("startEpoch", startEpoch);
        requireNonNegativeEpoch("endEpoch", endEpoch);
        if (endEpoch < startEpoch) {
            throw AdminApiException.badRequest("endEpoch " + endEpoch + " is before startEpoch " + startEpoch);
        }
        long epochs = (long) endEpoch - startEpoch + 1;
        int max = properties.getAdmin().getMaxRangeEpochs();
        if (epochs > max) {
            throw AdminApiException.badRequest("The range covers " + epochs + " epochs; at most " + max
                    + " are allowed per request (yaci.store.analytics.admin.max-range-epochs)");
        }
    }

    private void requireFinal(PartitionValue partition) {
        PartitionReadiness readiness = readinessService.checkFinal(partition);
        if (!readiness.isReady()) {
            throw new PartitionNotReadyException(readiness);
        }
    }

    private <T> T exclusive(Supplier<T> action) {
        if (!manualExportRunning.compareAndSet(false, true)) {
            throw AdminApiException.conflict("Another manual export or repair is running; try again when it has finished");
        }
        try {
            return action.get();
        } finally {
            manualExportRunning.set(false);
        }
    }

    private static ResponseEntity<ExportResult> toResponse(UniversalExportService.PartitionExportResult result) {
        return switch (result.outcome()) {
            case EXPORTED -> ResponseEntity.ok(new ExportResult(result.tableName(), result.partition(),
                    "Export completed successfully"));
            case NOT_READY -> throw new PartitionNotReadyException(result.readiness());
            case FAILED -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExportResult(
                    result.tableName(), result.partition(),
                    "Export failed, or another export of this partition was starting. See the application log; a "
                            + "failed partition is marked FAILED and retried by the scheduler"));
        };
    }

    // DTOs

    @Data
    public static class TableInfo {
        private final String tableName;
        private final String partitionStrategy;
        private final boolean enabled;
    }

    @Data
    public static class ExportResult {
        private final String tableName;
        private final String partition;
        private final String message;
    }

    @Data
    public static class BackfillResult {
        private final String tableName;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int successCount;
        private final int totalDays;
        /** Days skipped because the table was not ready for them. */
        private final List<String> notReady;
        /** Days whose export failed. */
        private final List<String> failed;
    }

    @Data
    public static class EpochBackfillResult {
        private final String tableName;
        private final int startEpoch;
        private final int endEpoch;
        private final int successCount;
        private final int totalEpochs;
        /** Epochs skipped because the table was not ready for them. */
        private final List<String> notReady;
        /** Epochs whose export failed. */
        private final List<String> failed;
    }
}
