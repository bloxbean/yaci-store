package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Export monitor that tracks scheduler liveness.
 *
 * Periodically checks:
 * 1. Scheduler liveness by comparing last activity timestamps to expected intervals
 * 2. Overall health status for monitoring endpoints
 *
 * Note: Stale IN_PROGRESS export recovery is handled by {@link StaleExportRecoveryService}.
 *
 * Enable/disable via: yaci.store.analytics.export-monitor.enabled=true|false
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.export-monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ExportMonitor {

    private final UniversalExportService universalExportService;
    private final ContinuousSyncScheduler continuousSyncScheduler;
    private final AnalyticsStoreProperties properties;

    /**
     * Periodic health check task.
     *
     * Runs at a configurable interval (default: 5 minutes) with an initial delay
     * of 120 seconds to allow the application to stabilize after startup.
     */
    @Scheduled(fixedRateString = "${yaci.store.analytics.export-monitor.check-interval-seconds:300}",
               initialDelay = 120,
               timeUnit = TimeUnit.SECONDS)
    public void checkExportHealth() {
        log.debug("Running export monitor health check");

        // Check scheduler liveness
        List<String> warnings = checkSchedulerLiveness();
        for (String warning : warnings) {
            log.warn("Scheduler liveness warning: {}", warning);
        }

        // Log summary
        ExportMonitorStatus status = getHealthStatus();
        if (status.isHealthy()) {
            log.debug("Export monitor health check passed (daily: {}, epoch: {}, sync: {})",
                    status.isDailyExportRunning() ? "running" : "idle",
                    status.isEpochExportRunning() ? "running" : "idle",
                    status.isContinuousSyncRunning() ? "running" : "idle");
        } else {
            log.warn("Export monitor health check: UNHEALTHY - {}", status.getUnhealthyReason());
        }
    }

    /**
     * Build current health status from scheduler state.
     *
     * @return ExportMonitorStatus with current scheduler information
     */
    public ExportMonitorStatus getHealthStatus() {
        List<String> reasons = new ArrayList<>();

        List<String> livenessWarnings = checkSchedulerLiveness();
        reasons.addAll(livenessWarnings);

        boolean healthy = reasons.isEmpty();
        String unhealthyReason = healthy ? null : String.join("; ", reasons);

        return ExportMonitorStatus.builder()
                .dailyExportRunning(universalExportService.isDailyExportRunning())
                .lastDailyExportStart(universalExportService.getLastDailyExportStart())
                .lastDailyExportEnd(universalExportService.getLastDailyExportEnd())
                .epochExportRunning(universalExportService.isEpochExportRunning())
                .lastEpochExportStart(universalExportService.getLastEpochExportStart())
                .lastEpochExportEnd(universalExportService.getLastEpochExportEnd())
                .continuousSyncRunning(continuousSyncScheduler.isSyncRunning())
                .lastSyncStart(continuousSyncScheduler.getLastSyncStart())
                .lastSyncEnd(continuousSyncScheduler.getLastSyncEnd())
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
