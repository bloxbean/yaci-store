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

    // If there's been no restart for this long, the attempt counter is reset so a new
    // outage starts fresh instead of inheriting the count of an old, resolved one.
    static final long RESTART_WINDOW_MS = 600000; // 10 minutes

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
            int attemptNumber = evaluateRestartAttempt(System.currentTimeMillis(), event);
            if (attemptNumber <= 0) {
                return; // debounced or rate-limited
            }

            long backoffMs = calculateBackoff(attemptNumber);

            log.info("Scheduling sync restart. Reason: {}, Attempt: {}, Backoff: {}ms",
                     event.getReason(), attemptNumber, backoffMs);

            // Wait for backoff
            Thread.sleep(backoffMs);

            // Perform restart
            performRestart(event);

        } catch (Exception e) {
            log.error("Error during sync restart", e);
        } finally {
            restartLock.unlock();
        }
    }

    /**
     * Applies the debounce window, the sliding-window counter reset and the
     * max-attempts limit. Returns the 1-based attempt number when a restart should go
     * ahead, or 0 to skip. {@code now} is a parameter so the windowing can be tested
     * without waiting.
     */
    int evaluateRestartAttempt(long now, RequiredSyncRestartEvent event) {
        long lastAttempt = lastRestartAttempt.get();

        if (now - lastAttempt < storeProperties.getAutoRestartDebounceWindowMs()) {
            log.info("Within debounce window. Ignoring restart event: {}", event.getReason());
            return 0;
        }

        // A gap longer than the window means the previous burst is over, so start counting again.
        if (lastAttempt > 0 && now - lastAttempt > RESTART_WINDOW_MS) {
            int previous = restartCount.getAndSet(0);
            if (previous > 0) {
                log.info("No restart attempt in the last {} ms. Resetting restart attempt counter from {} to 0.",
                         RESTART_WINDOW_MS, previous);
            }
        }

        if (restartCount.get() >= storeProperties.getAutoRestartMaxAttempts()) {
            log.warn("Reached max restart attempts ({}) within the last {} ms. " +
                     "Pausing further restarts until the window resets.",
                     storeProperties.getAutoRestartMaxAttempts(), RESTART_WINDOW_MS);
            return 0;
        }

        lastRestartAttempt.set(now);
        return restartCount.incrementAndGet();
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
    }

    private long calculateBackoff(int attemptNumber) {
        return Math.min(
            storeProperties.getAutoRestartBackoffBaseMs() * (long)Math.pow(2, attemptNumber - 1),
            60000L // Max 1 minute
        );
    }
}
