package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates exports, catalog maintenance and non-blocking catalog snapshots on the single
 * DuckLake writer connection.
 *
 * <p>{@link #acquire()} waits at most {@code yaci.store.analytics.ducklake.writer-lock-timeout-seconds}
 * and can be interrupted. An export that hangs (for example on a source query that never
 * returns) therefore makes the other exports and admin calls fail with a clear error, instead
 * of blocking their threads until the application is restarted.</p>
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeWriterLock {

    private final Semaphore permit = new Semaphore(1, true);
    private final Duration timeout;

    /** Lock with the default timeout. */
    public DuckLakeWriterLock() {
        this(Duration.ofSeconds(new AnalyticsStoreProperties.DuckLake().getWriterLockTimeoutSeconds()));
    }

    @Autowired
    public DuckLakeWriterLock(AnalyticsStoreProperties properties) {
        this(Duration.ofSeconds(properties.getDucklake().getWriterLockTimeoutSeconds()));
    }

    public DuckLakeWriterLock(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("DuckLake writer lock timeout must be positive: " + timeout);
        }
        this.timeout = timeout;
    }

    /**
     * Waits for the writer connection, at most the configured timeout.
     *
     * @throws WriterLockTimeoutException if another export or maintenance task still holds the
     *                                    connection after the timeout, or the wait was interrupted
     *                                    (the thread's interrupt flag is then set again)
     */
    public Guard acquire() {
        try {
            if (permit.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return new Guard();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WriterLockTimeoutException(
                    "Interrupted while waiting for the DuckLake writer connection", e);
        }
        throw new WriterLockTimeoutException(String.format(
                "Timed out after %ds waiting for the DuckLake writer connection: another export or "
                        + "maintenance task still holds it. Look for a long-running export in the logs, or raise "
                        + "yaci.store.analytics.ducklake.writer-lock-timeout-seconds if exports legitimately take longer",
                timeout.toSeconds()), null);
    }

    public Optional<Guard> tryAcquire() {
        return permit.tryAcquire() ? Optional.of(new Guard()) : Optional.empty();
    }

    public Duration getTimeout() {
        return timeout;
    }

    /** The DuckLake writer connection could not be obtained in time. */
    public static class WriterLockTimeoutException extends RuntimeException {
        public WriterLockTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public final class Guard implements AutoCloseable {
        private boolean closed;

        private Guard() {
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                permit.release();
            }
        }
    }
}
