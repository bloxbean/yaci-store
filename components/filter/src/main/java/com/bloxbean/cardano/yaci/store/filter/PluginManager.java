package com.bloxbean.cardano.yaci.store.filter;

import io.vertx.core.Vertx;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PluginManager {

    private final Vertx vertx;
    private final Map<String, Plugin> plugins = new HashMap<>();

    public PluginManager(Vertx vertx) {
        this.vertx = vertx;
    }

    public void registerPlugin(Plugin plugin) {
        plugin.initialize();
        plugins.put(plugin.getPluginId(), plugin);
    }

    public Object executePlugin(String pluginId, Object data) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            try {
                return plugin.execute(data); // Block until result is ready
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute plugin " + pluginId, e);
            }
        }
        throw new IllegalArgumentException("Plugin with ID " + pluginId + " not found.");
    }

    public void shutdownPlugin(String pluginId) {
        Plugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            plugin.shutdown();
        }
    }
}
