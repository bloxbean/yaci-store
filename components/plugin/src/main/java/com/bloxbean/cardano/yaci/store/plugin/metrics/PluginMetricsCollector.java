package com.bloxbean.cardano.yaci.store.plugin.metrics;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Dual-layer metrics collector for plugin execution tracking.
 * Layer 1: In-memory atomic counters for fast REST API queries
 * Layer 2: Micrometer registry for Prometheus/Grafana integration
 *
 */
@Service
@Slf4j
public class PluginMetricsCollector {

    private final StoreProperties storeProperties;
    private final MicrometerPluginMetrics micrometerMetrics;

    // In-memory metrics store (Layer 1)
    private final ConcurrentHashMap<String, PluginExecutionMetrics> metricsMap =
        new ConcurrentHashMap<>();

    // Active execution tracking for gauges
    private final AtomicInteger activeExecutions = new AtomicInteger(0);

    public PluginMetricsCollector(
            StoreProperties storeProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.storeProperties = storeProperties;

        // Initialize Micrometer layer if enabled and MeterRegistry available
        this.micrometerMetrics = meterRegistry != null && storeProperties.isPluginMetricsEnabled()
                ? new MicrometerPluginMetrics(meterRegistry)
                : null;

        // Register active executions gauge if Micrometer enabled
        if (micrometerMetrics != null) {
            micrometerMetrics.registerActiveExecutionsGauge(activeExecutions);
        }

        if (storeProperties.isPluginMetricsEnabled()) {
            log.info("Plugin metrics collector initialized (Micrometer: {})",
                    micrometerMetrics != null ? "enabled" : "disabled");
        } else {
            log.info("Plugin metrics collection disabled");
        }
    }

    /**
     * Record plugin execution (called from PluginAspect hot path).
     * PERFORMANCE CRITICAL: Keep this method minimal and lock-free.
     *
     * @param pluginName Plugin name/key
     * @param pluginType Plugin type (FILTER, SCHEDULER, etc.)
     * @param language Script language (mvel, groovy, kotlin, python)
     * @param startTime Execution start time in milliseconds
     * @param endTime Execution end time in milliseconds
     * @param success Whether execution was successful
     * @param error Exception if failed (null if success)
     */
    public void recordExecution(
            String pluginName,
            PluginType pluginType,
            String language,
            long startTime,
            long endTime,
            boolean success,
            Throwable error) {

        // Skip if metrics disabled
        if (!storeProperties.isPluginMetricsEnabled()) {
            return;
        }

        // Layer 1: In-memory (fast, always enabled if metrics enabled)
        PluginExecutionMetrics metrics = metricsMap.computeIfAbsent(
            pluginName,
            k -> new PluginExecutionMetrics(pluginName, pluginType, language)
        );
        metrics.recordExecution(startTime, endTime, success);

        // Layer 2: Micrometer (if enabled)
        if (micrometerMetrics != null) {
            micrometerMetrics.recordExecution(
                pluginName, pluginType, language,
                endTime - startTime, success
            );
        }
    }

    /**
     * Record items processed (for Filter/PreAction/PostAction plugins).
     * Called after batch processing to record how many items were processed.
     *
     * @param pluginName Plugin name/key
     * @param totalItems Total items processed
     */
    public void recordItemsProcessed(String pluginName, int totalItems) {
        if (!storeProperties.isPluginMetricsEnabled()) {
            return;
        }

        PluginExecutionMetrics metrics = metricsMap.get(pluginName);
        if (metrics != null) {
            metrics.recordItemsProcessed(totalItems);

            if (micrometerMetrics != null) {
                micrometerMetrics.recordItemsProcessed(
                    pluginName, metrics.getPluginType(), totalItems
                );
            }
        }
    }

    /**
     * Track active executions (for gauge).
     * Called before plugin execution starts.
     */
    public void incrementActiveExecutions() {
        activeExecutions.incrementAndGet();
    }

    /**
     * Track active executions (for gauge).
     * Called after plugin execution completes.
     */
    public void decrementActiveExecutions() {
        activeExecutions.decrementAndGet();
    }

    /**
     * Get all metrics (for REST API).
     *
     * @return Map of plugin name to metrics
     */
    public Map<String, PluginExecutionMetrics> getAllMetrics() {
        return new HashMap<>(metricsMap);
    }

    /**
     * Get metrics by type (for REST API).
     *
     * @param type Plugin type to filter by
     * @return Map of plugin name to metrics for specified type
     */
    public Map<String, PluginExecutionMetrics> getMetricsByType(PluginType type) {
        return metricsMap.entrySet().stream()
            .filter(e -> e.getValue().getPluginType() == type)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get single plugin metrics (for REST API).
     *
     * @param pluginName Plugin name/key
     * @return Optional containing metrics if found
     */
    public Optional<PluginExecutionMetrics> getMetrics(String pluginName) {
        return Optional.ofNullable(metricsMap.get(pluginName));
    }

    /**
     * Get total execution count by type (for overview).
     *
     * @param type Plugin type
     * @return Total executions for all plugins of specified type
     */
    public long getTotalExecutionsByType(PluginType type) {
        return metricsMap.values().stream()
            .filter(m -> m.getPluginType() == type)
            .mapToLong(m -> m.getExecutionCount().get())
            .sum();
    }

    /**
     * Get total error count by type (for overview).
     *
     * @param type Plugin type
     * @return Total errors for all plugins of specified type
     */
    public long getTotalErrorsByType(PluginType type) {
        return metricsMap.values().stream()
            .filter(m -> m.getPluginType() == type)
            .mapToLong(m -> m.getErrorCount().get())
            .sum();
    }

    /**
     * Initialize metrics entry for a newly registered plugin.
     * This ensures plugins appear in the API immediately upon registration,
     * even before they execute for the first time.
     *
     * @param pluginName Plugin name/key
     * @param pluginType Plugin type
     * @param language Script language
     */
    public void initializePlugin(String pluginName, PluginType pluginType, String language) {
        if (!storeProperties.isPluginMetricsEnabled()) {
            return;
        }

        // Create metrics entry with zero counts
        metricsMap.computeIfAbsent(
            pluginName,
            k -> new PluginExecutionMetrics(pluginName, pluginType, language)
        );
    }

}
