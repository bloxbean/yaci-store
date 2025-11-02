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
 * - Writer DataSource: For export operations
 * - Reader DataSource: Multiple connections for concurrent analytical queries
 *
 * For DuckDB catalog type: All connections point to the same catalog file
 * For PostgreSQL catalog type: In-memory connections with ATTACH to remote catalog
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
     * For DuckDB catalog: connects to catalog file (shared across connections)
     * For PostgreSQL catalog: in-memory with ATTACH
     */
    @Bean(name = "duckDbWriterDataSource")
    @ConfigurationProperties("yaci.store.analytics.duckdb.writer.datasource")
    public DataSource duckDbWriterDataSource() {
        String jdbcUrl = buildJdbcUrl();

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.duckdb.DuckDBDriver")
                .url(jdbcUrl)
                .build();

        return dataSource;
    }

    /**
     * Create DuckDB Reader DataSource with HikariCP connection pooling.
     *
     * Used for read-only analytical queries against DuckLake catalog.
     * Supports multiple concurrent readers for optimal query throughput.
     * Pool size defaults to available processor cores but can be configured.
     *
     * For DuckDB catalog: connects to catalog file (shared across connections)
     * For PostgreSQL catalog: in-memory with ATTACH
     *
     * Note: Read-only mode is enforced at the DuckDB ATTACH level (READ_ONLY option),
     * not at the connection level. DuckDB doesn't support connection-level read-only mode.
     */
    @Bean(name = "duckDbReaderDataSource")
    @ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
    public DataSource duckDbReaderDataSource() {
        String jdbcUrl = buildJdbcUrl();

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.duckdb.DuckDBDriver")
                .url(jdbcUrl)
                .build();

        // Configure pool size for concurrent reads
        int poolSize = properties.getDuckdb().getReader().getMaximumPoolSize();
        dataSource.setMaximumPoolSize(poolSize);
        dataSource.setMinimumIdle(Math.min(2, poolSize));

        return dataSource;
    }

    /**
     * Build JDBC URL based on catalog type.
     *
     * For DuckDB catalog: jdbc:duckdb:/path/to/catalog.db (all connections share file)
     *   - Catalog file is pre-initialized by DuckDbCatalogPreInitializer
     *   - All pooled connections connect to the same file
     *
     * For PostgreSQL catalog: jdbc:duckdb: (in-memory, uses ATTACH for catalog)
     */
    private String buildJdbcUrl() {
        if ("duckdb".equalsIgnoreCase(properties.getDucklake().getCatalogType())) {
            // Connect to catalog file - all connections share same database
            String catalogPath = properties.getDucklake().getCatalogPath();
            return "jdbc:duckdb:" + catalogPath;
        } else {
            // PostgreSQL catalog - use in-memory + ATTACH
            return "jdbc:duckdb:";
        }
    }
}
