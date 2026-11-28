package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A real DuckLake catalog for tests: a DuckDB catalog file under a temporary directory and an
 * in-memory DuckDB writer pool of one connection, like the shipped writer. No source database
 * is attached, so export queries are plain DuckDB queries.
 */
public final class DuckLakeTestCatalog implements AutoCloseable {

    public final AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
    public final HikariDataSource dataSource = new HikariDataSource();
    public final DuckDbConnectionHelper helper;
    public final DuckLakeWriterLock lock;
    public final String exportPath;

    public DuckLakeTestCatalog(Path dir) throws SQLException {
        this(dir, Duration.ofSeconds(30));
    }

    public DuckLakeTestCatalog(Path dir, Duration lockTimeout) throws SQLException {
        exportPath = dir.resolve("analytics").toString();
        properties.setExportPath(exportPath);
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(dir.resolve("analytics/ducklake.catalog.db").toString());

        dataSource.setJdbcUrl("jdbc:duckdb:");
        dataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);

        helper = new DuckDbConnectionHelper(environment(), properties) {
            @Override
            public void prepareConnectionForDuckLake(Connection conn, boolean needsSource, boolean readOnly)
                    throws SQLException {
                super.prepareConnectionForDuckLake(conn, false, readOnly);
            }
        };
        try (Connection conn = dataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            helper.configureDuckLakeCatalogSettings(conn);
        }
        lock = new DuckLakeWriterLock(lockTimeout);
    }

    public DuckLakeWriterService writer() {
        return new DuckLakeWriterService(dataSource, properties, helper, lock);
    }

    public String path(String table, String partition) {
        return Path.of(exportPath, table, partition, "data.parquet").toString();
    }

    public long count(String sql) throws SQLException {
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public void execute(String sql) throws SQLException {
        try (Connection conn = connection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /** The pool's single connection, attached and on ducklake_catalog. */
    public Connection connection() throws SQLException {
        Connection conn = dataSource.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("USE ducklake_catalog");
        }
        return conn;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    public static boolean duckLakeAvailable() {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSTALL ducklake");
            stmt.execute("LOAD ducklake");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static Environment environment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://localhost:5432/yaci?currentSchema=cardano");
        when(environment.getProperty("spring.datasource.username")).thenReturn("user");
        when(environment.getProperty("spring.datasource.password")).thenReturn("password");
        return environment;
    }
}
