package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * All connections are in-memory and ATTACH the DuckLake catalog via the
 * ducklake: protocol in prepareConnectionForDuckLake().
 */
@Configuration
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DuckDbDataSourceConfig {

    private final AnalyticsStoreProperties properties;

    /**
     * Create DuckDB Writer DataSource with HikariCP (pool size 1).
     *
     * HikariCP with pool size 1 provides critical mutual exclusion: the single connection
     * is exclusively held between getConnection() and close(), preventing concurrent access
     * to the DuckDB connection (which is NOT thread-safe).
     *
     * Connection health is validated on checkout via connectionTestQuery.
     * If the connection is stale/corrupted, HikariCP evicts it and creates a fresh one.
     */
    @Bean(name = "duckDbWriterDataSource")
    public DataSource duckDbWriterDataSource() {
        String jdbcUrl = buildJdbcUrl();

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.duckdb.DuckDBDriver")
                .url(jdbcUrl)
                .build();

        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTestQuery("SELECT 42");
        dataSource.setConnectionTimeout(60000);  // 60s - exports can take time
        dataSource.setMaxLifetime(0);             // No max lifetime (connection reused indefinitely)
        dataSource.setIdleTimeout(0);             // No idle timeout (keep connection alive)

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
     * Build JDBC URL — always uses in-memory connections.
     *
     * The DuckLake catalog (both DuckDB-file and PostgreSQL types) is
     * attached via the ducklake: protocol in prepareConnectionForDuckLake().
     * Connecting directly to the catalog file (jdbc:duckdb:{path}) opens it
     * as the main database WITHOUT DuckLake extension support, which causes
     * ATTACH conflicts and prevents the ducklake_catalog alias from being created.
     */
    private String buildJdbcUrl() {
        return "jdbc:duckdb:";
    }
}
