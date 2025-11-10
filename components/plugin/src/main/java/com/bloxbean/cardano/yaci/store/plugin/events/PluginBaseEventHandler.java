package com.bloxbean.cardano.yaci.store.plugin.events;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.EventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginMetricsCollector;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PluginBaseEventHandler {
    protected PluginRegistry pluginRegistry;
    protected StoreProperties storeProperties;
    protected PluginMetricsCollector metricsCollector;

    protected void handleEvent(Object  event) {
        List<EventHandlerPlugin<?>> eventHandlerPlugins = pluginRegistry.getEventHandlerPlugins(event.getClass().getSimpleName());
        if (eventHandlerPlugins == null || eventHandlerPlugins.isEmpty())
            return;

        for (EventHandlerPlugin<?> plugin : eventHandlerPlugins) {
            // Increment active executions
            if (metricsCollector != null) {
                metricsCollector.incrementActiveExecutions();
            }

            long startTime = System.currentTimeMillis();
            boolean success = false;

            try {
                plugin.handleEvent(event);
                success = true;
            } catch (Exception e) {
                log.error("Error in plugin " + plugin.getName() + ": " + e.getMessage(), e);
                boolean shouldExitOnError = plugin.getPluginDef().getExitOnError() != null
                    ? plugin.getPluginDef().getExitOnError()
                    : storeProperties.isPluginExitOnError();
                if (shouldExitOnError)
                    throw new RuntimeException("Plugin " + plugin.getName() + " failed to handle event: " + e.getMessage(), e);
            } finally {
                // Decrement active executions and record execution metrics
                if (metricsCollector != null) {
                    metricsCollector.decrementActiveExecutions();

                    long endTime = System.currentTimeMillis();
                    metricsCollector.recordExecution(
                        plugin.getName(),
                        PluginType.EVENT_HANDLER,
                        plugin.getPluginDef().getLang(),
                        startTime,
                        endTime,
                        success,
                        null
                    );
                }
            }
        }
    }
}
