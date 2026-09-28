package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Semaphore;

/** Coordinates exports and non-blocking catalog snapshots on the single DuckLake connection. */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeWriterLock {

    private final Semaphore permit = new Semaphore(1, true);

    public Guard acquire() {
        permit.acquireUninterruptibly();
        return new Guard();
    }

    public Optional<Guard> tryAcquire() {
        return permit.tryAcquire() ? Optional.of(new Guard()) : Optional.empty();
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
