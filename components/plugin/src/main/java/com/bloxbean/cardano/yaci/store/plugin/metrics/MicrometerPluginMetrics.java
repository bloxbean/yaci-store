package com.bloxbean.cardano.yaci.store.plugin.metrics;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer integration for plugin metrics.
 * Provides Prometheus-compatible metrics for monitoring and alerting.
 */
@Slf4j
public class MicrometerPluginMetrics {

    private final MeterRegistry meterRegistry;

    // Metric name constants
    private static final String EXECUTIONS_TOTAL = "yaci.plugin.executions.total";
    private static final String ERRORS_TOTAL = "yaci.plugin.errors.total";
    private static final String EXECUTION_DURATION = "yaci.plugin.execution.duration";
    private static final String ITEMS_PROCESSED_TOTAL = "yaci.plugin.items.processed.total";
    private static final String ACTIVE_COUNT = "yaci.plugin.active.count";

    public MicrometerPluginMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("Micrometer plugin metrics enabled - metrics will be available at /actuator/prometheus");
    }

    /**
     * Record plugin execution.
     * Uses lazy registration - metrics created on first execution per plugin.
     *
     * @param pluginName Plugin name/key
     * @param pluginType Plugin type
     * @param language Script language
     * @param durationMillis Execution duration in milliseconds
     * @param success Whether execution was successful
     */
    public void recordExecution(
            String pluginName,
            PluginType pluginType,
            String language,
            long durationMillis,
            boolean success) {

        String status = success ? "success" : "error";
        Tags baseTags = Tags.of(
            "plugin", pluginName,
            "type", pluginType.name(),
            "lang", language
        );

        // Counter: Total executions (with status tag)
        Counter.builder(EXECUTIONS_TOTAL)
            .description("Total plugin executions")
            .tags(baseTags.and("status", status))
            .register(meterRegistry)
            .increment();

        // Counter: Errors (only for failures)
        if (!success) {
            Counter.builder(ERRORS_TOTAL)
                .description("Total plugin errors")
                .tags(baseTags)
                .register(meterRegistry)
                .increment();
        }

        // Timer: Execution duration with percentiles (p50, p95, p99)
        // This automatically tracks count, total time, max, and percentiles
        Timer.builder(EXECUTION_DURATION)
            .description("Plugin execution duration in milliseconds")
            .tags(Tags.of("plugin", pluginName, "type", pluginType.name()))
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Record items processed (for Filter/PreAction/PostAction plugins).
     *
     * @param pluginName Plugin name/key
     * @param pluginType Plugin type
     * @param totalItems Total items processed
     */
    public void recordItemsProcessed(
            String pluginName,
            PluginType pluginType,
            int totalItems) {

        Tags tags = Tags.of("plugin", pluginName, "type", pluginType.name());

        // Counter: Items processed
        if (totalItems > 0) {
            Counter.builder(ITEMS_PROCESSED_TOTAL)
                .description("Total items processed by plugin")
                .tags(tags)
                .register(meterRegistry)
                .increment(totalItems);
        }
    }

    /**
     * Register gauge for active executions.
     * Called during collector initialization.
     *
     * @param activeExecutions AtomicInteger tracking active executions
     */
    public void registerActiveExecutionsGauge(AtomicInteger activeExecutions) {
        Gauge.builder(ACTIVE_COUNT, activeExecutions, AtomicInteger::get)
            .description("Number of currently executing plugins")
            .register(meterRegistry);

        log.debug("Registered gauge: {} for active plugin executions", ACTIVE_COUNT);
    }
}
