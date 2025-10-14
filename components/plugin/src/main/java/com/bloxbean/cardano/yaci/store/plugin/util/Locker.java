package com.bloxbean.cardano.yaci.store.plugin.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class Locker {
    private final ConcurrentHashMap<String, ReentrantLock> namedLocks = new ConcurrentHashMap<>();
    private static final String GLOBAL_LOCK_NAME = "__global__";

    /**
     * Execute operation with a named lock for coordination between plugins.
     * This is more script-friendly than using object-based locks.
     *
     * @param lockName The name of the lock (e.g., "database_cleanup", "transaction_processing")
     * @param operation The operation to execute under the lock
     * @return The result of the operation
     *
     * Usage examples:
     * - MVEL: result = locker.withLock("my_lock", () -> processData())
     * - JavaScript: const result = locker.withLock("my_lock", () => processData());
     * - Python: result = locker.withLock("my_lock", lambda: process_data())
     */
    public <T> T withLock(String lockName, Supplier<T> operation) {
        ReentrantLock lock = getOrCreateLock(lockName);
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute operation with the global lock (simplest synchronization).
     * Uses a special global named lock for coordination across all plugins.
     *
     * @param operation The operation to execute under the global lock
     * @return The result of the operation
     */
    public <T> T withGlobalLock(Supplier<T> operation) {
        return withLock(GLOBAL_LOCK_NAME, operation);
    }

    /**
     * Try to execute operation with a named lock, with timeout support.
     * Returns null if lock cannot be acquired within the timeout.
     *
     * @param lockName The name of the lock
     * @param operation The operation to execute
     * @param timeoutSeconds Maximum time to wait for the lock (in seconds)
     * @return The result of the operation, or null if timeout occurred
     *
     * Usage examples:
     * - MVEL: result = locker.tryWithLock("api_calls", () -> callAPI(), 10)
     * - JavaScript: const result = locker.tryWithLock("api_calls", () => callAPI(), 10);
     * - Python: result = locker.tryWithLock("api_calls", lambda: call_api(), 10)
     */
    public <T> T tryWithLock(String lockName, Supplier<T> operation, int timeoutSeconds) {
        ReentrantLock lock = getOrCreateLock(lockName);
        try {
            if (lock.tryLock(timeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    return operation.get();
                } finally {
                    lock.unlock();
                }
            }
            return null; // Timeout occurred
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Execute operation with a named lock, with timeout support.
     * Throws RuntimeException if lock cannot be acquired within timeout.
     *
     * @param lockName The name of the lock
     * @param operation The operation to execute
     * @param timeoutSeconds Maximum time to wait for the lock (in seconds)
     * @return The result of the operation
     * @throws RuntimeException if timeout occurs
     */
    public <T> T withLockTimeout(String lockName, Supplier<T> operation, int timeoutSeconds) {
        T result = tryWithLock(lockName, operation, timeoutSeconds);
        if (result == null && !Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Failed to acquire lock '" + lockName + "' within " + timeoutSeconds + " seconds");
        }
        return result;
    }

    // ===== Void Operations (No Return Value) =====

    /**
     * Execute operation with a named lock (for operations that don't return values).
     *
     * @param lockName The name of the lock
     * @param operation The operation to execute
     */
    public void withLockVoid(String lockName, Runnable operation) {
        ReentrantLock lock = getOrCreateLock(lockName);
        lock.lock();
        try {
            operation.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute operation with global lock (for operations that don't return values).
     * Uses a special global named lock for coordination across all plugins.
     *
     * @param operation The operation to execute
     */
    public void withGlobalLockVoid(Runnable operation) {
        withLockVoid(GLOBAL_LOCK_NAME, operation);
    }

    /**
     * Try to execute operation with a named lock and timeout (no return value).
     *
     * @param lockName The name of the lock
     * @param operation The operation to execute
     * @param timeoutSeconds Maximum time to wait for the lock (in seconds)
     * @return true if operation completed, false if timeout occurred
     */
    public boolean tryWithLockVoid(String lockName, Runnable operation, int timeoutSeconds) {
        ReentrantLock lock = getOrCreateLock(lockName);
        try {
            if (lock.tryLock(timeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    operation.run();
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false; // Timeout occurred
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ===== Lock State Query Methods =====

    /**
     * Check if a named lock is currently held by any thread.
     * Useful for debugging and coordination logic.
     *
     * @param lockName The name of the lock to check
     * @return true if the lock exists and is currently held
     */
    public boolean isLocked(String lockName) {
        ReentrantLock lock = namedLocks.get(lockName);
        return lock != null && lock.isLocked();
    }

    /**
     * Get the number of threads waiting for a named lock.
     *
     * @param lockName The name of the lock to check
     * @return The number of waiting threads, or 0 if lock doesn't exist
     */
    public int getWaitingCount(String lockName) {
        ReentrantLock lock = namedLocks.get(lockName);
        return lock != null ? lock.getQueueLength() : 0;
    }

    /**
     * Get the total number of named locks currently managed.
     *
     * @return The number of named locks
     */
    public int getNamedLockCount() {
        return namedLocks.size();
    }

    /**
     * Clear all named locks. Use with caution - only for cleanup scenarios.
     * This will remove references to locks but won't force-unlock them.
     */
    public void clearNamedLocks() {
        namedLocks.clear();
    }

    // ===== Helper Methods =====

    private ReentrantLock getOrCreateLock(String lockName) {
        return namedLocks.computeIfAbsent(lockName, k -> new ReentrantLock());
    }

}

