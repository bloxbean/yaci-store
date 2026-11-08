package com.bloxbean.cardano.yaci.store.core.processor;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
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
    private final StoreProperties storeProperties;

    private final AtomicLong lastRestartAttempt = new AtomicLong(0);
    private final AtomicInteger restartCount = new AtomicInteger(0);
    private final Lock restartLock = new ReentrantLock();

    // Pause between stop and start, so the old session can shut down. Not final so tests can shorten it.
    long shutdownWaitMs = 5000;
    // Delay of the follow-up check after a restart request was dropped or failed. 0: use the debounce window.
    long retryCheckDelayMs = 0;
    private final AtomicBoolean retryCheckScheduled = new AtomicBoolean(false);

    @EventListener
    public void handleRequiredRestart(RequiredSyncRestartEvent event) {
        Thread.startVirtualThread(() -> handleRestartInterval(event));
    }

    void handleRestartInterval(RequiredSyncRestartEvent event) {
        if (!storeProperties.isAutoRestartEnabled()) {
            log.warn("Auto-restart is disabled. Ignoring event: {}", event.getReason());
            return;
        }

        if (!restartLock.tryLock()) {
            log.info("Restart already in progress. Ignoring event: {}", event.getReason());
            scheduleRetryCheck(event);
            return;
        }

        try {
            int attemptNumber = evaluateRestartAttempt(System.currentTimeMillis(), event);
            if (attemptNumber <= 0) {
                scheduleRetryCheck(event);
                return; // debounced or rate-limited
            }

            long backoffMs = calculateBackoff(attemptNumber);

            log.info("Scheduling sync restart. Reason: {}, Attempt: {}, Backoff: {}ms",
                     event.getReason(), attemptNumber, backoffMs);

            // Read before the backoff, so a deliberate stop during the wait is seen
            long stopCountBefore = startService.getStopCount();

            // Wait for backoff
            Thread.sleep(backoffMs);

            // Perform restart
            performRestart(event, stopCountBefore);

        } catch (Exception e) {
            log.error("Error during sync restart", e);
            scheduleRetryCheck(event);
        } finally {
            restartLock.unlock();
        }
    }

    /**
     * A restart request that was dropped (debounce, another restart running, attempt limit) or a restart that
     * failed must not be the end of it: the health check that would ask again is optional and off by default.
     * Check again once the debounce window has passed, and restart if the sync is still stopped by an error.
     * At most one check is pending at a time.
     */
    private void scheduleRetryCheck(RequiredSyncRestartEvent event) {
        if (!retryCheckScheduled.compareAndSet(false, true))
            return;

        long delayMs = retryCheckDelayMs > 0
                ? retryCheckDelayMs
                : Math.max(storeProperties.getAutoRestartDebounceWindowMs(), 1000L);
        Thread.startVirtualThread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                retryCheckScheduled.set(false);
                return;
            }
            retryCheckScheduled.set(false);

            if (startService.isSyncFailed()) {
                log.info("Sync is still stopped after an error. Trying the restart again for: {}", event.getReason());
                handleRestartInterval(event);
            }
        });
    }

    /**
     * Applies the debounce window, the sliding-window counter reset and the
     * max-attempts limit. Returns the 1-based attempt number when a restart should go
     * ahead, or 0 to skip. {@code now} is a parameter so the windowing can be tested
     * without waiting.
     */
    int evaluateRestartAttempt(long now, RequiredSyncRestartEvent event) {
        long lastAttemptEpochMs = lastRestartAttempt.get();
        long windowMs = storeProperties.getAutoRestartWindowMs();

        if (now - lastAttemptEpochMs < storeProperties.getAutoRestartDebounceWindowMs()) {
            log.info("Within debounce window. Ignoring restart event: {}", event.getReason());
            return 0;
        }

        // A gap longer than the window means the previous burst is over, so start counting again.
        if (lastAttemptEpochMs > 0 && now - lastAttemptEpochMs > windowMs) {
            int previous = restartCount.getAndSet(0);
            if (previous > 0) {
                log.info("No restart attempt in the last {} ms. Resetting restart attempt counter from {} to 0.",
                         windowMs, previous);
            }
        }

        if (restartCount.get() >= storeProperties.getAutoRestartMaxAttempts()) {
            long retryIntervalMs = storeProperties.getAutoRestartRetryIntervalAfterMaxMs();
            if (retryIntervalMs <= 0) {
                log.warn("Reached max restart attempts ({}) within the last {} ms. " +
                         "Pausing further restarts until the window resets.",
                         storeProperties.getAutoRestartMaxAttempts(), windowMs);
                return 0;
            }

            // Keep trying, but slowly. Pausing for the whole window kept the sync down for minutes after the
            // fault had already cleared.
            if (now - lastAttemptEpochMs < retryIntervalMs) {
                log.warn("Reached max restart attempts ({}) within the last {} ms. " +
                         "Next restart allowed {} ms after the previous one.",
                         storeProperties.getAutoRestartMaxAttempts(), windowMs, retryIntervalMs);
                return 0;
            }
        }

        lastRestartAttempt.set(now);
        return restartCount.incrementAndGet();
    }

    void performRestart(RequiredSyncRestartEvent event, long stopCountBefore) {
        log.info("Stopping sync service...");
        long stopToken = startService.stopForRestart(stopCountBefore);
        if (stopToken < 0)
            return;

        // Wait for clean shutdown
        try {
            TimeUnit.MILLISECONDS.sleep(shutdownWaitMs);
            log.debug("Waited {} ms for clean shutdown", shutdownWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for shutdown");
        }

        log.info("Starting sync service...");
        // If this start fails, startAfterRestart leaves the sync in an error state rather than "scheduled to stop",
        // so the health check tries again (#959).
        if (!startService.startAfterRestart(stopToken))
            return;

        log.info("Sync restart completed for reason: {}", event.getReason());
    }

    long calculateBackoff(int attemptNumber) {
        return Math.min(
            // The exponent is capped: attempts keep counting past max-attempts during a long outage, and an
            // uncapped 2^n overflows to a negative sleep, which would stop the auto-restart for good.
            storeProperties.getAutoRestartBackoffBaseMs() * (1L << Math.min(Math.max(attemptNumber - 1, 0), 16)),
            60000L // Max 1 minute
        );
    }
}
