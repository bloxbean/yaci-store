package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles recovery of stale IN_PROGRESS exports on startup and cleanup on shutdown.
 *
 * On startup:
 * - Detects exports that have been IN_PROGRESS longer than the configured stale timeout
 *   (e.g., due to a previous crash) and resets them to FAILED so they can be retried.
 *
 * On shutdown:
 * - Resets all IN_PROGRESS exports to FAILED to prevent them from being stuck
 *   after the application stops.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class StaleExportRecoveryService {

    private final ExportStateService stateService;
    private final AnalyticsStoreProperties properties;

    /**
     * On startup, reset any stale IN_PROGRESS exports that exceeded the timeout.
     * This handles the case where the application crashed during a previous export.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resetStaleExportsOnStartup() {
        int timeoutMinutes = properties.getStateManagement().getStaleTimeoutMinutes();
        log.info("Checking for stale IN_PROGRESS exports (timeout: {} minutes)...", timeoutMinutes);

        int resetCount = stateService.resetStaleInProgressExports(timeoutMinutes);

        if (resetCount > 0) {
            log.warn("Reset {} stale IN_PROGRESS exports on startup", resetCount);
        } else {
            log.info("No stale IN_PROGRESS exports found");
        }
    }

    /**
     * On shutdown, reset all IN_PROGRESS exports to FAILED.
     * This prevents exports from being stuck in IN_PROGRESS state after the application stops.
     */
    @PreDestroy
    public void cleanupOnShutdown() {
        log.info("Application shutting down - resetting any IN_PROGRESS exports...");

        try {
            int resetCount = stateService.resetAllInProgressExports();
            if (resetCount > 0) {
                log.warn("Reset {} IN_PROGRESS exports during shutdown", resetCount);
            }
        } catch (Exception e) {
            log.error("Failed to reset IN_PROGRESS exports during shutdown: {}", e.getMessage(), e);
        }
    }
}
