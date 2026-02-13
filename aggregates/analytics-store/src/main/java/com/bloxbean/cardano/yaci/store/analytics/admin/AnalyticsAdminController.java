package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ContinuousSyncScheduler;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportMonitorStatus;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportMonitor;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.UniversalExportScheduler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
 */
@RestController
@RequestMapping("/api/v1/analytics/admin")
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.admin", name = "enabled", havingValue = "true")
public class AnalyticsAdminController {

    private final UniversalExportScheduler universalExportScheduler;
    private final ContinuousSyncScheduler continuousSyncScheduler;
    private final TableExporterRegistry registry;
    private final ExportStateAdminService adminService;
    private final ExportMonitor exportMonitor;

    public AnalyticsAdminController(
            UniversalExportScheduler universalExportScheduler,
            ContinuousSyncScheduler continuousSyncScheduler,
            TableExporterRegistry registry,
            ExportStateAdminService adminService,
            @Autowired(required = false) ExportMonitor exportMonitor) {
        this.universalExportScheduler = universalExportScheduler;
        this.continuousSyncScheduler = continuousSyncScheduler;
        this.registry = registry;
        this.adminService = adminService;
        this.exportMonitor = exportMonitor;
    }

    /**
     * List all registered table exporters.
     *
     * @return List of table information
     */
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
    @PostMapping("/export/date/{date}")
    public ResponseEntity<ExportResult> exportDate(@PathVariable String date) {
        LocalDate exportDate = LocalDate.parse(date);
        log.info("Manual export triggered for all daily tables for date: {}", exportDate);

        int successCount = universalExportScheduler.exportAllDailyTables(exportDate);
        List<String> enabledTables = registry.getEnabledTablesByStrategy(PartitionStrategy.DAILY);

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
    @PostMapping("/export/table/{tableName}/date/{date}")
    public ResponseEntity<ExportResult> exportTableDate(
            @PathVariable String tableName,
            @PathVariable String date) {

        LocalDate exportDate = LocalDate.parse(date);
        log.info("Manual export triggered for table {} for date: {}", tableName, exportDate);

        boolean success = universalExportScheduler.exportTable(tableName, PartitionValue.ofDate(exportDate));

        return ResponseEntity.ok(new ExportResult(
                tableName,
                date,
                success ? "Export completed successfully" : "Export failed"
        ));
    }

    /**
     * Export specific table for a specific epoch.
     *
     * @param tableName Table name
     * @param epoch Epoch number
     * @return Export result
     */
    @PostMapping("/export/table/{tableName}/epoch/{epoch}")
    public ResponseEntity<ExportResult> exportTableEpoch(
            @PathVariable String tableName,
            @PathVariable int epoch) {

        log.info("Manual export triggered for table {} for epoch: {}", tableName, epoch);

        boolean success = universalExportScheduler.exportTable(tableName, PartitionValue.ofEpoch(epoch));

        return ResponseEntity.ok(new ExportResult(
                tableName,
                "epoch=" + epoch,
                success ? "Export completed successfully" : "Export failed"
        ));
    }

    /**
     * Export specific table for a date range.
     *
     * @param tableName Table name
     * @param startDate Start date (inclusive, yyyy-MM-dd format)
     * @param endDate End date (inclusive, yyyy-MM-dd format)
     * @return Backfill result
     */
    @PostMapping("/export/table/{tableName}/range")
    public ResponseEntity<BackfillResult> exportTableRange(
            @PathVariable String tableName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        log.info("Manual range export triggered for table {}: {} to {}", tableName, start, end);
        int successCount = universalExportScheduler.exportDateRange(tableName, start, end);

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;

        return ResponseEntity.ok(new BackfillResult(
                tableName,
                start,
                end,
                successCount,
                (int) totalDays
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
    @PostMapping("/export/table/{tableName}/epoch-range")
    public ResponseEntity<EpochBackfillResult> exportTableEpochRange(
            @PathVariable String tableName,
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {

        log.info("Manual epoch range export triggered for table {}: {} to {}", tableName, startEpoch, endEpoch);
        int successCount = universalExportScheduler.exportEpochRange(tableName, startEpoch, endEpoch);

        int totalEpochs = endEpoch - startEpoch + 1;

        return ResponseEntity.ok(new EpochBackfillResult(
                tableName,
                startEpoch,
                endEpoch,
                successCount,
                totalEpochs
        ));
    }

    /**
     * Get current sync status.
     *
     * @return SyncStatus with current sync information
     */
    @GetMapping("/status")
    public ResponseEntity<ContinuousSyncScheduler.SyncStatus> getStatus() {
        return ResponseEntity.ok(continuousSyncScheduler.getSyncStatus());
    }

    /**
     * Get scheduler health status.
     *
     * Reports current state of all schedulers and any stale exports.
     * Returns 404 if export monitor is disabled.
     *
     * @return ExportMonitorStatus with scheduler state and health information
     */
    @GetMapping("/health")
    public ResponseEntity<ExportMonitorStatus> getHealth() {
        if (exportMonitor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(exportMonitor.getHealthStatus());
    }

    /**
     * Get export statistics for a specific table.
     *
     * @param tableName Table name
     * @return ExportStatistics with aggregate information
     */
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
     * @param partitionValue Partition value (e.g., "2024-01-15" or "450")
     * @return Success message
     */
    @DeleteMapping("/state/{tableName}/{partitionValue}")
    public ResponseEntity<String> resetState(
            @PathVariable String tableName,
            @PathVariable String partitionValue) {

        log.warn("Resetting export state for table {} partition: {}", tableName, partitionValue);
        adminService.resetExportState(tableName, partitionValue);
        return ResponseEntity.ok("State reset successfully");
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
    @DeleteMapping("/state/{tableName}/range")
    public ResponseEntity<String> resetStateRange(
            @PathVariable String tableName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        log.warn("Resetting export state range for table {}: {} to {}", tableName, startDate, endDate);
        int count = adminService.resetDateRange(tableName, startDate, endDate);
        return ResponseEntity.ok(String.format("Reset %d partition states", count));
    }

    /**
     * Trigger immediate gap sync check.
     *
     * Bypasses the scheduled interval and runs gap detection immediately.
     *
     * @return Success message
     */
    @PostMapping("/sync/trigger")
    public ResponseEntity<String> triggerSync() {
        log.info("Manual sync triggered via API");
        continuousSyncScheduler.syncGaps();
        return ResponseEntity.ok("Sync triggered");
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
    }

    @Data
    public static class EpochBackfillResult {
        private final String tableName;
        private final int startEpoch;
        private final int endEpoch;
        private final int successCount;
        private final int totalEpochs;
    }
}
