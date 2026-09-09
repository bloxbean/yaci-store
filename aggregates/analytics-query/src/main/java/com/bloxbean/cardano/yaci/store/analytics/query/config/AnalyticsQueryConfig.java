package com.bloxbean.cardano.yaci.store.analytics.query.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.analytics.query")
public class AnalyticsQueryConfig {
}
