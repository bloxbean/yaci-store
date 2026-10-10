package com.bloxbean.cardano.yaci.store.starter.core.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.Map;

/** Runs migrations before the consuming application's context or processors are created. */
public class SchemaOnlyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (application.getAllSources().contains(MigrationApplication.class)
                || !environment.getProperty("store.schema-only", Boolean.class, false)) {
            return;
        }
        runMigrations(environment);
        // This is an explicit one-shot process mode. The original application never starts.
        System.exit(0);
    }

    static void runMigrations(ConfigurableEnvironment source) {
        var modules = com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig.forApplication(source,
                Thread.currentThread().getContextClassLoader());
        var environment = new StandardEnvironment();
        environment.setActiveProfiles(source.getActiveProfiles());
        environment.setDefaultProfiles(source.getDefaultProfiles());
        for (PropertySource<?> propertySource : source.getPropertySources()) {
            environment.getPropertySources().addLast(propertySource);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("schema-only-mode", Map.of(
                "store.schema-only", "false",
                "store.sync-auto-start", "false",
                "spring.flyway.enabled", "true",
                "spring.main.web-application-type", "none",
                "spring.main.lazy-initialization", "false",
                "spring.main.sources", "")));
        var migration = new SpringApplication(MigrationApplication.class);
        migration.setDefaultProperties(Map.of(
                "spring.flyway.locations", "classpath:db/store/{vendor}",
                "spring.flyway.out-of-order", "true",
                "spring.flyway.fail-on-missing-locations", "true"));
        migration.setEnvironment(environment);
        migration.setWebApplicationType(WebApplicationType.NONE);
        migration.setRegisterShutdownHook(false);
        migration.setLogStartupInfo(false);
        try (var context = migration.run()) {
            var flyway = context.getBean(org.flywaydb.core.Flyway.class);
            var dataSource = flyway.getConfiguration().getDataSource();
            try (var connection = dataSource.getConnection()) {
                com.bloxbean.cardano.yaci.store.common.config.SchemaProfile.write(connection,
                        flyway.getConfiguration().getDefaultSchema() != null ? flyway.getConfiguration().getDefaultSchema()
                                : connection.getSchema() != null ? connection.getSchema() : connection.getCatalog(),
                        modules);
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException("Unable to record schema store configuration", e);
            }
            System.out.println("Schema-only initialization complete. Chain sync and store jobs were not started.");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
    static class MigrationApplication {}
}
