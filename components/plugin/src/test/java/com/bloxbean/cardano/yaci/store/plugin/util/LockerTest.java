package com.bloxbean.cardano.yaci.store.plugin.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LockerTest {

    private Locker locker;

    @BeforeEach
    void setUp() {
        locker = new Locker();
    }

    @Test
    void testWithLockBasic() {
        String result = locker.withLock("test-lock", () -> "result");
        assertEquals("result", result);
    }

    @Test
    void testWithLockConcurrency() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Start 10 threads that will increment counter under lock
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    locker.withLock("counter-lock", () -> {
                        int current = counter.get();
                        // Simulate some work
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        counter.set(current + 1);
                        return current + 1;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(10, counter.get()); // Should be exactly 10 if properly synchronized

        executor.shutdown();
    }

    @Test
    void testWithGlobalLock() {
        String result = locker.withGlobalLock(() -> "global");
        assertEquals("global", result);
    }

    @Test
    void testTryWithLockSuccess() {
        String result = locker.tryWithLock("try-lock", () -> "success", 1);
        assertEquals("success", result);
    }

    @Test
    void testTryWithLockTimeout() throws InterruptedException {
        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch testStartLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // First thread holds the lock
        executor.submit(() -> {
            locker.withLock("timeout-lock", () -> {
                lockHeldLatch.countDown(); // Signal that lock is held
                try {
                    testStartLatch.await(5, TimeUnit.SECONDS); // Wait for test to proceed
                    Thread.sleep(3000); // Hold lock for 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "first";
            });
        });

        // Wait for first thread to acquire lock
        assertTrue(lockHeldLatch.await(2, TimeUnit.SECONDS));

        // Second thread tries to acquire lock with 1 second timeout
        testStartLatch.countDown();
        String result = locker.tryWithLock("timeout-lock", () -> "second", 1);

        assertNull(result); // Should timeout and return null

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testWithLockTimeout() {
        assertThrows(RuntimeException.class, () -> {
            CountDownLatch lockHeldLatch = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // First thread holds the lock
            executor.submit(() -> {
                locker.withLock("timeout-exception-lock", () -> {
                    lockHeldLatch.countDown();
                    try {
                        Thread.sleep(3000); // Hold for 3 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "first";
                });
            });

            try {
                lockHeldLatch.await(2, TimeUnit.SECONDS);
                // This should throw RuntimeException after 1 second timeout
                locker.withLockTimeout("timeout-exception-lock", () -> "second", 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdown();
            }
        });
    }

    // ===== Test Void Operations =====

    @Test
    void testWithLockVoid() {
        AtomicBoolean executed = new AtomicBoolean(false);
        locker.withLockVoid("void-lock", () -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void testWithGlobalLockVoid() {
        AtomicBoolean executed = new AtomicBoolean(false);
        locker.withGlobalLockVoid(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void testTryWithLockVoidSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = locker.tryWithLockVoid("void-try-lock", () -> executed.set(true), 1);
        assertTrue(result);
        assertTrue(executed.get());
    }

    @Test
    void testTryWithLockVoidTimeout() throws InterruptedException {
        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch testStartLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // First thread holds the lock
        executor.submit(() -> {
            locker.withLockVoid("void-timeout-lock", () -> {
                lockHeldLatch.countDown();
                try {
                    testStartLatch.await(5, TimeUnit.SECONDS);
                    Thread.sleep(3000); // Hold for 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        assertTrue(lockHeldLatch.await(2, TimeUnit.SECONDS));
        testStartLatch.countDown();

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = locker.tryWithLockVoid("void-timeout-lock", () -> executed.set(true), 1);

        assertFalse(result); // Should timeout
        assertFalse(executed.get()); // Should not execute

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    // ===== Test Lock State Query Methods =====

    @Test
    void testIsLockedAndLockState() throws InterruptedException {
        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        assertFalse(locker.isLocked("state-test-lock")); // Initially not locked

        executor.submit(() -> {
            locker.withLock("state-test-lock", () -> {
                lockHeldLatch.countDown();
                try {
                    releaseLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            });
        });

        assertTrue(lockHeldLatch.await(2, TimeUnit.SECONDS));
        assertTrue(locker.isLocked("state-test-lock")); // Should be locked now

        releaseLatch.countDown(); // Release the lock
        Thread.sleep(100); // Give time for lock to be released
        assertFalse(locker.isLocked("state-test-lock")); // Should be unlocked now

        executor.shutdown();
    }

    @Test
    void testGetWaitingCount() throws InterruptedException {
        CountDownLatch lockHeldLatch = new CountDownLatch(1);
        CountDownLatch waitersReadyLatch = new CountDownLatch(3);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // First thread holds the lock
        executor.submit(() -> {
            locker.withLock("waiting-count-lock", () -> {
                lockHeldLatch.countDown();
                try {
                    releaseLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "holder";
            });
        });

        // Wait for lock to be held
        assertTrue(lockHeldLatch.await(2, TimeUnit.SECONDS));

        // Start 3 waiting threads
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                waitersReadyLatch.countDown();
                return locker.withLock("waiting-count-lock", () -> "waiter");
            });
        }

        // Wait for all waiters to start
        assertTrue(waitersReadyLatch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100); // Give time for waiters to queue up

        assertEquals(3, locker.getWaitingCount("waiting-count-lock"));

        releaseLatch.countDown(); // Release the lock
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testGetNamedLockCount() {
        assertEquals(0, locker.getNamedLockCount());

        locker.withLock("lock1", () -> "test");
        assertEquals(1, locker.getNamedLockCount());

        locker.withLock("lock2", () -> "test");
        assertEquals(2, locker.getNamedLockCount());

        locker.withLock("lock1", () -> "test"); // Same lock, count shouldn't increase
        assertEquals(2, locker.getNamedLockCount());
    }

    @Test
    void testClearNamedLocks() {
        locker.withLock("lock1", () -> "test");
        locker.withLock("lock2", () -> "test");
        assertEquals(2, locker.getNamedLockCount());

        locker.clearNamedLocks();
        assertEquals(0, locker.getNamedLockCount());
    }

    // ===== Test Error Handling =====

    @Test
    void testExceptionInOperation() {
        assertThrows(RuntimeException.class, () -> {
            locker.withLock("exception-lock", () -> {
                throw new RuntimeException("Test exception");
            });
        });

        // Lock should be released even after exception
        String result = locker.withLock("exception-lock", () -> "success");
        assertEquals("success", result);
    }

    @Test
    void testInterruption() throws InterruptedException {
        CountDownLatch interruptedLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                locker.withLock("interrupt-lock", () -> {
                    try {
                        Thread.sleep(10000); // Long sleep
                        return "not interrupted";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        interruptedLatch.countDown();
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException e) {
                // Expected when interrupted
            }
        });

        Thread.sleep(100); // Give thread time to start
        executor.shutdownNow(); // Interrupt the thread
        assertTrue(interruptedLatch.await(2, TimeUnit.SECONDS));
    }

    // ===== Test Different Lock Names =====

    @Test
    void testDifferentLockNamesAreIndependent() throws InterruptedException {
        CountDownLatch lock1Held = new CountDownLatch(1);
        CountDownLatch lock2Acquired = new CountDownLatch(1);
        CountDownLatch releaseLock1 = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1 holds lock1
        executor.submit(() -> {
            locker.withLock("independent-lock-1", () -> {
                lock1Held.countDown();
                try {
                    releaseLock1.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "lock1";
            });
        });

        // Thread 2 should be able to acquire lock2 even though lock1 is held
        executor.submit(() -> {
            try {
                lock1Held.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            locker.withLock("independent-lock-2", () -> {
                lock2Acquired.countDown();
                return "lock2";
            });
        });

        assertTrue(lock1Held.await(2, TimeUnit.SECONDS));
        assertTrue(lock2Acquired.await(2, TimeUnit.SECONDS)); // Should succeed quickly

        releaseLock1.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
