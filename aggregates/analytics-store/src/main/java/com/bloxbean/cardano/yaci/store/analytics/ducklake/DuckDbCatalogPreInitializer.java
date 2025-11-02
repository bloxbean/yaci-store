package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Pre-initializes DuckDB catalog file before DataSources are created.
 *
 * This component runs with highest precedence to ensure the DuckDB catalog file
 * exists before HikariCP connection pools try to connect to it.
 *
 * On first startup:
 * - Creates catalog file via temporary in-memory connection
 * - Uses ATTACH 'ducklake:...' to initialize catalog structure
 * - Closes temporary connection
 *
 * On subsequent startups:
 * - Detects existing catalog file
 * - Skips initialization
 *
 * This allows multiple pooled connections to connect to the same catalog file
 * without file locking conflicts.
 */
@Component
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DuckDbCatalogPreInitializer implements InitializingBean {

    private final AnalyticsStoreProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        // Only for DuckDB catalog type (PostgreSQL uses ATTACH, doesn't need pre-init)
        if (!"duckdb".equalsIgnoreCase(properties.getDucklake().getCatalogType())) {
            log.debug("PostgreSQL catalog type - skipping pre-initialization");
            return;
        }

        String catalogPath = properties.getDucklake().getCatalogPath();
        File catalogFile = new File(catalogPath);

        if (catalogFile.exists()) {
            log.info("DuckDB catalog already exists: {}", catalogPath);
            return;
        }

        log.info("Pre-initializing DuckDB catalog at: {}", catalogPath);

        // Ensure catalog directory exists
        Path catalogDir = Paths.get(catalogPath).getParent();
        if (catalogDir != null && !Files.exists(catalogDir)) {
            try {
                Files.createDirectories(catalogDir);
                log.debug("Created catalog directory: {}", catalogDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create catalog directory: " + e.getMessage(), e);
            }
        }

        // Ensure data path directory exists
        String dataPath = properties.getExportPath();
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
                log.debug("Created data directory: {}", dataDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data directory: " + e.getMessage(), e);
            }
        }

        // Create catalog file using temporary in-memory connection
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            // Install ducklake extension
            executeSql(conn, "INSTALL ducklake;");
            log.debug("Installed ducklake extension");

            executeSql(conn, "LOAD ducklake;");
            log.debug("Loaded ducklake extension");

            // ATTACH ducklake to create catalog file
            String attachSql = String.format(
                    "ATTACH 'ducklake:%s' AS ducklake_catalog (DATA_PATH '%s');",
                    catalogPath,
                    dataPath
            );
            executeSql(conn, attachSql);

            log.info("✅ DuckDB catalog pre-initialized successfully: {}", catalogPath);
            log.info("   Data path: {}", dataPath);

        } catch (SQLException e) {
            log.error("Failed to pre-initialize DuckDB catalog: {}", e.getMessage(), e);
            throw new RuntimeException("DuckDB catalog pre-initialization failed", e);
        }
    }

    /**
     * Execute SQL statement with proper error handling.
     */
    private void executeSql(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Failed to execute SQL: {} - {}", sql, e.getMessage());
            throw e;
        }
    }
}
