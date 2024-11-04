package com.bloxbean.cardano.yaci.store.vertx;

import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.vertx"})
public class VertxConfig {

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }
}
