package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Flyway configuration for Analytics module.
 *
 * Explicitly configures Flyway to use the default/primary DataSource (PostgreSQL/H2/MySQL)
 * and prevents it from trying to use the DuckDB DataSource.
 */
@Configuration
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class PrimaryDataSourceConfig {

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource primaryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
            @Qualifier("dataSource") DataSource dataSource) {
        return configuration -> configuration.dataSource(dataSource);
    }
}
