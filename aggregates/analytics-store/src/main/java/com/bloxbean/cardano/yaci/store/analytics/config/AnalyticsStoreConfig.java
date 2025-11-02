package com.bloxbean.cardano.yaci.store.analytics.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.analytics")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.analytics.state")
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.analytics.state")
@EnableScheduling
public class AnalyticsStoreConfig {
}
