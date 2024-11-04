package com.bloxbean.cardano.yaci.store.filter;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.filter"})
public class FilterConfig {

    private final String pluginId = "filterPlugin";
    @Value("${store.utxo.filter.path: null}")
    private String scriptPath;

    @Bean
    public VertxJsPlugin vertxJsPlugin(Vertx vertx) {
        return new VertxJsPlugin(vertx, pluginId, scriptPath);
    }

    @Bean
    public EnhancedGraalvmPlugin enhancedGraalvmPlugin() {
        return new EnhancedGraalvmPlugin(pluginId, scriptPath, "js");
    }
}
