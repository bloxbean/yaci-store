package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for the DuckDB writer DataSource.
 *
 * <p>A single pooled in-memory DuckDB connection is used for export operations. It ATTACHes
 * the DuckLake catalog via the {@code ducklake:} protocol in
 * {@code prepareConnectionForDuckLake()} and keeps it attached for the JVM lifetime.</p>
 *
 * <p>There is deliberately no separate reader pool: DuckDB allows exactly one DuckDB instance
 * per process to have a given catalog file open ({@code Unique file handle conflict}),
 * READ_ONLY included. Catalog metadata for read-only consumers is obtained through this
 * writer connection ({@code DuckLakeCatalogSnapshotReader}); all serving reads happen in the
 * analytics query layer, which reads the committed Parquet files directly.</p>
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

        String initSql = buildConnectionInitSql();
        if (initSql != null) {
            dataSource.setConnectionInitSql(initSql);
        }

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

    /**
     * Build SQL to configure DuckDB instance settings.
     * Executed once per new physical connection by HikariCP's connectionInitSql.
     */
    private String buildConnectionInitSql() {
        String memoryLimit = properties.getDuckdb().getMemoryLimit();
        if (memoryLimit != null && !memoryLimit.isBlank()) {
            return "SET memory_limit = '" + memoryLimit + "'";
        }
        return null;
    }
}
