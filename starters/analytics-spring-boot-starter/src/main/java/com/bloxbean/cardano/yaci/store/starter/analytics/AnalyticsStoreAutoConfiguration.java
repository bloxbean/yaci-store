package com.bloxbean.cardano.yaci.store.starter.analytics;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreConfig;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.config.AnalyticsQueryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot AutoConfiguration for Analytics Store and Analytics Query modules.
 *
 * Automatically imports the configs when the starter is included in the classpath.
 * Each module is enabled/disabled via its own property:
 * yaci.store.analytics.enabled and yaci.store.analytics.query.enabled.
 */
@AutoConfiguration
@EnableConfigurationProperties(AnalyticsStoreProperties.class)
@Import({AnalyticsStoreConfig.class, AnalyticsQueryConfig.class})
@Slf4j
public class AnalyticsStoreAutoConfiguration {
}
