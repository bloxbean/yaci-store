package com.bloxbean.cardano.yaci.store.plugin.cache;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import org.springframework.stereotype.Service;

@Service
public class PluginCacheService {
    private final Cache<String, Object> globalCache;
    private final Cache<String, Cache<String, Object>> pluginCaches;

    public PluginCacheService(Cache<String, Object> globalCache,
                              Cache<String, Cache<String, Object>> pluginCaches
    ) {
        this.globalCache  = globalCache;
        this.pluginCaches = pluginCaches;
    }

    /**
     * @return the one global cache shared by all plugins
     */
    public Cache<String, Object> global() {
        return globalCache;
    }

    /**
     * @param pluginKey a unique identifier for your plugin (e.g. "utxo.save", "metadata.save")
     * @return a dedicated cache for that plugin
     */
    public Cache<String, Object> forPlugin(String pluginKey) {
        return pluginCaches.computeIfAbsent(pluginKey, k -> new ConcurrentMapCache<>());
    }
}

