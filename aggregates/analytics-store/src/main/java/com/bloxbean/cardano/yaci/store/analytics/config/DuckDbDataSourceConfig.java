package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration for the DuckDB writer DataSource.
 *
 * <p>A single pooled in-memory DuckDB connection ({@code jdbc:duckdb:}) is used for export
 * operations. The catalog is never opened as the main database: connecting directly to the
 * catalog file opens it without DuckLake support. The connection ATTACHes
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
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("duckdb-writer");
        // Every physical connection is a new in-memory DuckDB instance with its own temp
        // directory, memory_limit, threads and extension directories (see DuckDbInstances).
        // Closing an evicted connection removes only that instance's spill files.
        dataSource.setDataSource(DuckDbInstances.dataSource(properties, DuckDbInstances.Role.WRITER));

        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTestQuery("SELECT 42");
        dataSource.setConnectionTimeout(60000);  // 60s - exports can take time
        dataSource.setMaxLifetime(0);             // No max lifetime (connection reused indefinitely)
        dataSource.setIdleTimeout(0);             // No idle timeout (keep connection alive)

        return dataSource;
    }
}
