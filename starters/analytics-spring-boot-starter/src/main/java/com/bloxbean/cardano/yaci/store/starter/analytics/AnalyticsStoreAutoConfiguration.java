package com.bloxbean.cardano.yaci.store.starter.analytics;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreConfig;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot AutoConfiguration for Analytics Store module.
 *
 * Automatically imports AnalyticsStoreConfig when the starter is included in the classpath.
 * The analytics module is enabled/disabled via yaci.store.analytics.enabled property.
 */
@AutoConfiguration
@EnableConfigurationProperties(AnalyticsStoreProperties.class)
@Import({AnalyticsStoreConfig.class})
@Slf4j
public class AnalyticsStoreAutoConfiguration {
}
