package com.bloxbean.cardano.yaci.store.starter.analytics;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreConfig;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.config.AnalyticsQueryConfig;
import com.bloxbean.cardano.yaci.store.mcp.server.config.McpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot AutoConfiguration for Analytics Store, Analytics Query, and MCP Server modules.
 *
 * Automatically imports all configs when the starter is included in the classpath.
 * Each module is enabled/disabled via its own property.
 */
@AutoConfiguration
@EnableConfigurationProperties(AnalyticsStoreProperties.class)
@Import({AnalyticsStoreConfig.class, AnalyticsQueryConfig.class, McpServerConfig.class})
@Slf4j
public class AnalyticsStoreAutoConfiguration {
}
