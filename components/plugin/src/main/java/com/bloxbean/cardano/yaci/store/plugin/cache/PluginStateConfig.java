package com.bloxbean.cardano.yaci.store.plugin.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginStateConfig {

    @Bean
    public State<String, Object> globalState() {
        return new ConcurrentMapState<>();
    }

    @Bean
    public State<String,
            State<String, Object>> pluginStates() {
        return new ConcurrentMapState<>();
    }
}
