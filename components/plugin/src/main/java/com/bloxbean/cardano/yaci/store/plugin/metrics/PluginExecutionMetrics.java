package com.bloxbean.cardano.yaci.store.plugin.metrics;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lock-free metrics for plugin execution tracking.
 * Optimized for high-frequency updates with minimal overhead.
 */
@Getter
public class PluginExecutionMetrics {

    private final String pluginName;
    private final PluginType pluginType;
    private final String language;

    // Lock-free atomic counters for execution tracking
    private final AtomicLong executionCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    // For high-frequency plugins (filters) - LongAdder has better performance under contention
    private final LongAdder itemsProcessed = new LongAdder();

    // Timing metrics
    private final AtomicLong lastExecutionTimeMillis = new AtomicLong(0);
    private final AtomicLong lastDurationMillis = new AtomicLong(0);
    private final AtomicLong minDurationMillis = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDurationMillis = new AtomicLong(0);
    private final AtomicLong totalDurationMillis = new AtomicLong(0);

    public PluginExecutionMetrics(String pluginName, PluginType pluginType, String language) {
        this.pluginName = pluginName;
        this.pluginType = pluginType;
        this.language = language;
    }

    /**
     * Record a plugin execution.
     * PERFORMANCE CRITICAL: This method is called on the hot path for every plugin execution.
     *
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @param success Whether the execution was successful
     */
    public void recordExecution(long startTime, long endTime, boolean success) {
        long duration = endTime - startTime;

        // Update counters
        executionCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        } else {
            errorCount.incrementAndGet();
        }

        // Update timing metrics
        lastExecutionTimeMillis.set(startTime);
        lastDurationMillis.set(duration);
        totalDurationMillis.addAndGet(duration);

        // Update min/max with lock-free compare-and-swap
        updateMin(minDurationMillis, duration);
        updateMax(maxDurationMillis, duration);
    }

    /**
     * Record items processed (for Filter/PreAction/PostAction plugins).
     *
     * @param count Number of items processed
     */
    public void recordItemsProcessed(int count) {
        itemsProcessed.add(count);
    }

    /**
     * Get average execution duration in milliseconds.
     *
     * @return Average duration, or 0 if no executions
     */
    public double getAverageDuration() {
        long count = executionCount.get();
        return count > 0 ? (double) totalDurationMillis.get() / count : 0;
    }

    /**
     * Get success rate as a percentage (0.0 to 1.0).
     *
     * @return Success rate, or 0 if no executions
     */
    public double getSuccessRate() {
        long count = executionCount.get();
        return count > 0 ? (double) successCount.get() / count : 0;
    }

    /**
     * Get total items processed.
     *
     * @return Total items processed
     */
    public long getTotalItemsProcessed() {
        return itemsProcessed.sum();
    }

    /**
     * Get minimum execution duration.
     *
     * @return Minimum duration, or 0 if no executions
     */
    public long getMinDuration() {
        long min = minDurationMillis.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * Get maximum execution duration.
     *
     * @return Maximum duration
     */
    public long getMaxDuration() {
        return maxDurationMillis.get();
    }

    /**
     * Reset all metrics (for testing).
     */
    public void reset() {
        executionCount.set(0);
        successCount.set(0);
        errorCount.set(0);
        itemsProcessed.reset();
        lastExecutionTimeMillis.set(0);
        lastDurationMillis.set(0);
        minDurationMillis.set(Long.MAX_VALUE);
        maxDurationMillis.set(0);
        totalDurationMillis.set(0);
    }

    /**
     * Lock-free update of minimum value using compare-and-swap.
     */
    private void updateMin(AtomicLong atomic, long newValue) {
        long current;
        do {
            current = atomic.get();
            if (newValue >= current) {
                return; // New value is not smaller, no update needed
            }
        } while (!atomic.compareAndSet(current, newValue));
    }

    /**
     * Lock-free update of maximum value using compare-and-swap.
     */
    private void updateMax(AtomicLong atomic, long newValue) {
        long current;
        do {
            current = atomic.get();
            if (newValue <= current) {
                return; // New value is not larger, no update needed
            }
        } while (!atomic.compareAndSet(current, newValue));
    }
}
