package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuckLakeWriterLockTest {

    @Test
    void acquireGivesUpAfterTheTimeout() throws Exception {
        DuckLakeWriterLock lock = new DuckLakeWriterLock(Duration.ofMillis(200));
        try (DuckLakeWriterLock.Guard ignored = lock.acquire()) {
            long start = System.nanoTime();
            // Another thread: the semaphore is not reentrant either way
            CompletableFuture<Throwable> waiter = CompletableFuture.supplyAsync(() -> {
                try {
                    lock.acquire().close();
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            });
            Throwable error = waiter.get(10, TimeUnit.SECONDS);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(error).isInstanceOf(DuckLakeWriterLock.WriterLockTimeoutException.class)
                    .hasMessageContaining("writer-lock-timeout-seconds");
            assertThat(elapsedMs).isBetween(150L, 5_000L);
        }
        // Released: available again
        lock.acquire().close();
    }

    @Test
    void aWaitingThreadCanBeInterrupted() throws Exception {
        DuckLakeWriterLock lock = new DuckLakeWriterLock(Duration.ofMinutes(10));
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean interruptFlag = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);

        try (DuckLakeWriterLock.Guard ignored = lock.acquire()) {
            Thread waiter = new Thread(() -> {
                try {
                    lock.acquire().close();
                } catch (Throwable t) {
                    error.set(t);
                    interruptFlag.set(Thread.currentThread().isInterrupted());
                } finally {
                    done.countDown();
                }
            });
            waiter.start();
            Thread.sleep(100);
            waiter.interrupt();
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(error.get()).isInstanceOf(DuckLakeWriterLock.WriterLockTimeoutException.class)
                .hasMessageContaining("Interrupted");
        assertThat(interruptFlag).isTrue();
    }

    @Test
    void timeoutComesFromTheProperties() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        assertThat(new DuckLakeWriterLock(properties).getTimeout()).isEqualTo(Duration.ofSeconds(900));
        properties.getDucklake().setWriterLockTimeoutSeconds(5);
        assertThat(new DuckLakeWriterLock(properties).getTimeout()).isEqualTo(Duration.ofSeconds(5));
        properties.getDucklake().setWriterLockTimeoutSeconds(0);
        assertThatThrownBy(() -> new DuckLakeWriterLock(properties)).isInstanceOf(IllegalArgumentException.class);
    }
}
