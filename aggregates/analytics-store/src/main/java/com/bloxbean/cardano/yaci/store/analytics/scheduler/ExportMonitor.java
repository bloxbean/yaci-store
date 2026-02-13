package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Export monitor that tracks scheduler health and recovers stuck exports.
 *
 * Periodically checks:
 * 1. Stale IN_PROGRESS exports (stuck beyond staleTimeoutMinutes) and marks them as FAILED
 * 2. Scheduler liveness by comparing last activity timestamps to expected intervals
 * 3. Overall health status for monitoring endpoints
 *
 * Enable/disable via: yaci.store.analytics.export-monitor.enabled=true|false
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.export-monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ExportMonitor {

    private final UniversalExportScheduler universalExportScheduler;
    private final ContinuousSyncScheduler continuousSyncScheduler;
    private final ExportStateRepository exportStateRepository;
    private final ExportStateService exportStateService;
    private final AnalyticsStoreProperties properties;

    /**
     * Periodic health check and recovery task.
     *
     * Runs at a configurable interval (default: 5 minutes) with an initial delay
     * of 120 seconds to allow the application to stabilize after startup.
     */
    @Scheduled(fixedRateString = "${yaci.store.analytics.export-monitor.check-interval-seconds:300}",
               initialDelay = 120,
               timeUnit = TimeUnit.SECONDS)
    public void checkHealthAndRecover() {
        log.debug("Running export monitor health check");

        // 1. Find and recover stale IN_PROGRESS exports
        int staleTimeoutMinutes = properties.getStateManagement().getStaleTimeoutMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTimeoutMinutes);

        List<ExportState> staleExports = exportStateRepository.findStaleInProgressExports(threshold);

        if (!staleExports.isEmpty()) {
            log.warn("Found {} stale IN_PROGRESS exports (older than {} minutes), marking as FAILED",
                    staleExports.size(), staleTimeoutMinutes);

            for (ExportState staleExport : staleExports) {
                String tableName = staleExport.getId().getTableName();
                String partition = staleExport.getId().getPartitionValue();
                log.warn("Recovering stale export: {}/{} (started at: {})",
                        tableName, partition, staleExport.getStartedAt());
                exportStateService.markFailed(staleExport,
                        "Recovered by export monitor: export stale for over " + staleTimeoutMinutes + " minutes");
            }
        }

        // 2. Check scheduler liveness
        List<String> warnings = checkSchedulerLiveness();
        for (String warning : warnings) {
            log.warn("Scheduler liveness warning: {}", warning);
        }

        // 3. Log summary
        ExportMonitorStatus status = getHealthStatus();
        if (status.isHealthy()) {
            log.debug("Export monitor health check passed (stale: {}, daily: {}, epoch: {}, sync: {})",
                    status.getStaleInProgressCount(),
                    status.isDailyExportRunning() ? "running" : "idle",
                    status.isEpochExportRunning() ? "running" : "idle",
                    status.isContinuousSyncRunning() ? "running" : "idle");
        } else {
            log.warn("Export monitor health check: UNHEALTHY - {}", status.getUnhealthyReason());
        }
    }

    /**
     * Build current health status from scheduler state and stale export count.
     *
     * @return ExportMonitorStatus with current scheduler information
     */
    public ExportMonitorStatus getHealthStatus() {
        int staleTimeoutMinutes = properties.getStateManagement().getStaleTimeoutMinutes();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTimeoutMinutes);
        int staleCount = exportStateRepository.findStaleInProgressExports(threshold).size();

        List<String> reasons = new ArrayList<>();
        if (staleCount > 0) {
            reasons.add(staleCount + " stale IN_PROGRESS exports detected");
        }

        List<String> livenessWarnings = checkSchedulerLiveness();
        reasons.addAll(livenessWarnings);

        boolean healthy = reasons.isEmpty();
        String unhealthyReason = healthy ? null : String.join("; ", reasons);

        return ExportMonitorStatus.builder()
                .dailyExportRunning(universalExportScheduler.getIsDailyExporting().get())
                .lastDailyExportStart(universalExportScheduler.getLastDailyExportStart())
                .lastDailyExportEnd(universalExportScheduler.getLastDailyExportEnd())
                .epochExportRunning(universalExportScheduler.getIsEpochExporting().get())
                .lastEpochExportStart(universalExportScheduler.getLastEpochExportStart())
                .lastEpochExportEnd(universalExportScheduler.getLastEpochExportEnd())
                .continuousSyncRunning(continuousSyncScheduler.getIsSyncing().get())
                .lastSyncStart(continuousSyncScheduler.getLastSyncStart())
                .lastSyncEnd(continuousSyncScheduler.getLastSyncEnd())
                .staleInProgressCount(staleCount)
                .healthy(healthy)
                .unhealthyReason(unhealthyReason)
                .build();
    }

    /**
     * Check scheduler liveness by comparing last activity to expected intervals.
     * Warns if a scheduler hasn't completed in more than 3x its expected interval.
     */
    private List<String> checkSchedulerLiveness() {
        List<String> warnings = new ArrayList<>();

        // Check continuous sync liveness (expected every syncCheckIntervalMinutes)
        Instant lastSyncEnd = continuousSyncScheduler.getLastSyncEnd();
        if (lastSyncEnd != null) {
            int expectedIntervalMinutes = properties.getContinuousSync().getSyncCheckIntervalMinutes();
            Duration sinceLast = Duration.between(lastSyncEnd, Instant.now());
            long thresholdMinutes = (long) expectedIntervalMinutes * 3;

            if (sinceLast.toMinutes() > thresholdMinutes) {
                warnings.add(String.format(
                        "Continuous sync has not completed in %d minutes (expected every %d minutes)",
                        sinceLast.toMinutes(), expectedIntervalMinutes));
            }
        }

        return warnings;
    }
}