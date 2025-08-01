package com.bloxbean.cardano.yaci.store.core.processor;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class RequiredRestartProcessor {
    private final StartService startService;
    private final HealthService healthService;
    private final StoreProperties storeProperties;

    private final AtomicLong lastRestartAttempt = new AtomicLong(0);
    private final AtomicInteger restartCount = new AtomicInteger(0);
    private final Lock restartLock = new ReentrantLock();
    private final AtomicBoolean monitoringHealth = new AtomicBoolean(false);

    // Track successful sync period
    private static final long SUCCESS_THRESHOLD_MS = 300000; // 5 minutes
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds

    @EventListener
    public void handleRequiredRestart(RequiredSyncRestartEvent event) {

        Thread.startVirtualThread(() -> handleRestartInterval(event));
    }

    private void handleRestartInterval(RequiredSyncRestartEvent event) {
        if (!storeProperties.isAutoRestartEnabled()) {
            log.warn("Auto-restart is disabled. Ignoring event: {}", event.getReason());
            return;
        }

        if (!restartLock.tryLock()) {
            log.info("Restart already in progress. Ignoring event: {}", event.getReason());
            return;
        }

        try {
            // Check debounce window
            long now = System.currentTimeMillis();
            long lastAttempt = lastRestartAttempt.get();
            if (now - lastAttempt < storeProperties.getAutoRestartDebounceWindowMs()) {
                log.info("Within debounce window. Ignoring restart event: {}", event.getReason());
                return;
            }

            // Check retry limit
            if (restartCount.get() >= storeProperties.getAutoRestartMaxAttempts()) {
                log.error("Max restart attempts reached. Manual intervention required.");
                return;
            }

            // Calculate backoff
            int attemptNumber = restartCount.incrementAndGet();
            long backoffMs = calculateBackoff(attemptNumber);

            log.info("Scheduling sync restart. Reason: {}, Attempt: {}, Backoff: {}ms",
                     event.getReason(), attemptNumber, backoffMs);

            // Wait for backoff
            Thread.sleep(backoffMs);

            // Perform restart
            performRestart(event);

            lastRestartAttempt.set(System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Error during sync restart", e);
        } finally {
            restartLock.unlock();
        }
    }

    private void performRestart(RequiredSyncRestartEvent event) {
        log.info("Stopping sync service...");
        startService.stop();

        // Wait for clean shutdown
        try {
            TimeUnit.SECONDS.sleep(5);
            log.debug("Waited 5 seconds for clean shutdown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for shutdown");
        }

        log.info("Starting sync service...");
        startService.start();

        log.info("Sync restart completed for reason: {}", event.getReason());

        // Start health monitoring thread only if we have restart attempts
        if (restartCount.get() > 0) {
            startHealthMonitoring();
        }
    }

    private long calculateBackoff(int attemptNumber) {
        return Math.min(
            storeProperties.getAutoRestartBackoffBaseMs() * (long)Math.pow(2, attemptNumber - 1),
            60000L // Max 1 minute
        );
    }

    private void startHealthMonitoring() {
        // Only start monitoring if not already running
        if (!monitoringHealth.compareAndSet(false, true)) {
            log.debug("Health monitoring already in progress");
            return;
        }

        Thread.startVirtualThread(() -> {
            log.info("Starting health monitoring to reset restart counter after stable sync");
            long monitoringStartTime = System.currentTimeMillis();
            long lastSuccessfulSyncTime = 0;

            try {
                while (monitoringHealth.get() && restartCount.get() > 0) {
                    // Check sync health using HealthService
                    var healthStatus = healthService.getHealthStatus();
                    
                    if (healthStatus.isConnectionAlive() && !healthStatus.isError()) {
                        long lastBlockTime = healthStatus.getLastReceivedBlockTime();
                        long currentTime = System.currentTimeMillis();

                        // Check if we're receiving blocks (within last minute)
                        if (lastBlockTime > 0 && (currentTime - lastBlockTime) < 60000) {
                            // We're successfully syncing
                            if (lastSuccessfulSyncTime == 0) {
                                lastSuccessfulSyncTime = currentTime;
                                log.debug("Started tracking successful sync period");
                            } else if (currentTime - lastSuccessfulSyncTime > SUCCESS_THRESHOLD_MS) {
                                // 5 minutes of stable sync, reset counter
                                log.info("Sync has been stable for 5 minutes. Resetting restart counter.");
                                restartCount.set(0);
                                break; // Exit monitoring
                            }
                        } else {
                            // Not receiving blocks, reset success tracking
                            lastSuccessfulSyncTime = 0;
                        }
                    } else {
                        // Not running or in error state, reset success tracking
                        lastSuccessfulSyncTime = 0;
                    }

                    // Stop monitoring after 10 minutes regardless
                    if (System.currentTimeMillis() - monitoringStartTime > 600000) {
                        log.info("Health monitoring timeout reached (10 minutes). Stopping monitoring.");
                        break;
                    }

                    // Sleep for check interval
                    try {
                        TimeUnit.MILLISECONDS.sleep(HEALTH_CHECK_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                monitoringHealth.set(false);
                log.info("Health monitoring stopped. Restart counter: {}", restartCount.get());
            }
        });
    }
}
