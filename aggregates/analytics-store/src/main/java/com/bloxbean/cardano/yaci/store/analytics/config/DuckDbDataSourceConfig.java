package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for DuckDB DataSources with connection pooling.
 *
 * Creates separate pooled DataSources for DuckDB:
 * - Writer DataSource: Single connection for exports (file locking requirement)
 * - Reader DataSource: Multiple connections for concurrent analytical queries
 */
@Configuration
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DuckDbDataSourceConfig {

    private final AnalyticsStoreProperties properties;

    /**
     * Create DuckDB Writer DataSource with HikariCP connection pooling.
     *
     * Used for export operations that write to DuckLake catalog.
     * Pool size limited to 1 for DuckDB file-based catalogs due to file locking.
     * PostgreSQL catalogs can use larger pool sizes.
     */
    @Bean(name = "duckDbWriterDataSource")
    @ConfigurationProperties("yaci.store.analytics.duckdb.writer.datasource")
    public DataSource duckDbWriterDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.duckdb.DuckDBDriver")
                .url("jdbc:duckdb:")
                .build();

        // For DuckDB file-based catalogs, limit pool to 1 connection (file locking limitation)
        if ("ducklake".equalsIgnoreCase(properties.getStorage().getType()) &&
            "duckdb".equalsIgnoreCase(properties.getDucklake().getCatalogType())) {
            dataSource.setMaximumPoolSize(1);
            dataSource.setMinimumIdle(1);
        }

        return dataSource;
    }

    /**
     * Create DuckDB Reader DataSource with HikariCP connection pooling.
     *
     * Used for read-only analytical queries against DuckLake catalog.
     * Supports multiple concurrent readers for optimal query throughput.
     * Pool size defaults to available processor cores but can be configured.
     *
     * Note: Read-only mode is enforced at the DuckDB ATTACH level (READ_ONLY option),
     * not at the connection level. DuckDB doesn't support connection-level read-only mode.
     */
    @Bean(name = "duckDbReaderDataSource")
    @ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
    public DataSource duckDbReaderDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.duckdb.DuckDBDriver")
                .url("jdbc:duckdb:")
                .build();

        // Configure pool size for concurrent reads
        int poolSize = properties.getDuckdb().getReader().getMaximumPoolSize();
        dataSource.setMaximumPoolSize(poolSize);
        dataSource.setMinimumIdle(Math.min(2, poolSize));

        return dataSource;
    }
}
