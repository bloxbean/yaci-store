package com.bloxbean.cardano.yaci.store.plugin.cache;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginCacheConfig {

    @Bean
    public Cache<String, Object> globalCache() {
        return new ConcurrentMapCache<>();
    }

    @Bean
    public Cache<String,
            Cache<String, Object>> pluginCaches() {
        return new ConcurrentMapCache<>();
    }
}
