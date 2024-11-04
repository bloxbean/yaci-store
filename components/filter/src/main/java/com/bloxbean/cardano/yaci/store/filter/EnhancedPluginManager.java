package com.bloxbean.cardano.yaci.store.filter;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EnhancedPluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();

    public void registerPlugin(Plugin plugin) {
        plugin.initialize();
        plugins.put(plugin.getPluginId(), plugin);
    }

    public Object executePlugin(String pluginId, Object data) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }
        return plugin.execute(data);
    }

    public void shutdownPlugin(String pluginId) {
        Plugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            plugin.shutdown();
        }
    }
}
