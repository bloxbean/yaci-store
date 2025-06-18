package com.bloxbean.cardano.yaci.store.plugin.events;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.EventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PluginBaseEventHandler {
    protected PluginRegistry pluginRegistry;
    protected StoreProperties storeProperties;

    protected void handleEvent(Object  event) {
        List<EventHandlerPlugin<?>> eventHandlerPlugins = pluginRegistry.getEventHandlerPlugins(event.getClass().getSimpleName());
        if (eventHandlerPlugins == null || eventHandlerPlugins.isEmpty())
            return;

        for (EventHandlerPlugin<?> plugin : eventHandlerPlugins) {
            try {
                plugin.handleEvent(event);
            } catch (Exception e) {
                log.error("Error in plugin " + plugin.getName() + ": " + e.getMessage(), e);
                if (storeProperties.isPluginExitOnError() || plugin.getPluginDef().isExitOnError())
                    throw new RuntimeException("Plugin " + plugin.getName() + " failed to handle event: " + e.getMessage(), e);
            }
        }
    }
}
