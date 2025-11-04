package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DuckLake catalog initialization service.
 *
 * Initializes the DuckLake catalog on application startup:
 * - Creates catalog directory (for DuckDB catalog)
 * - Creates catalog schema (for PostgreSQL catalog)
 * - Validates catalog connectivity
 * - Installs required DuckDB extensions (ducklake, postgres_scanner)
 *
 * This service runs once on application startup when DuckLake storage is enabled.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeCatalogInitializer {

    private final DataSource duckDbDataSource;
    private final DuckDbConnectionHelper connectionHelper;
    private final AnalyticsStoreProperties properties;

    public DuckLakeCatalogInitializer(
            @Qualifier("duckDbWriterDataSource") DataSource duckDbDataSource,
            DuckDbConnectionHelper connectionHelper,
            AnalyticsStoreProperties properties) {
        this.duckDbDataSource = duckDbDataSource;
        this.connectionHelper = connectionHelper;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        log.info("Initializing DuckLake catalog (type: {})...", properties.getDucklake().getCatalogType());

        try (Connection conn = duckDbDataSource.getConnection()) {
            // For PostgreSQL catalogs, ensure schema exists BEFORE attaching
            if ("postgresql".equalsIgnoreCase(properties.getDucklake().getCatalogType())) {
                log.info("Creating catalog schema 'public' if not exists (required by DuckLake)...");
                connectionHelper.ensureCatalogSchemaExists(conn);
            }

            // Prepare connection for DuckLake (installs extensions, attaches catalog)
            // No source attachment needed for initialization, readOnly=false
            connectionHelper.prepareConnectionForDuckLake(conn, false, false);

            // Verify catalog is accessible
            verifyCatalog(conn);

            // Configure catalog compression settings
            connectionHelper.configureDuckLakeCatalogSettings(conn);

            log.info("✅ DuckLake catalog initialized successfully");
            log.debug("DuckDB connection will close now, releasing any attached PostgreSQL connections");

        } catch (SQLException e) {
            log.error("❌ Failed to initialize DuckLake catalog: {}", e.getMessage(), e);
            throw new RuntimeException("DuckLake catalog initialization failed", e);
        }

        log.info("Catalog initialization complete, connection closed");
    }

    /**
     * Verify catalog is accessible.
     */
    private void verifyCatalog(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Try to query catalog metadata using duckdb_tables()
            stmt.executeQuery("SELECT COUNT(*) FROM duckdb_tables() WHERE database_name = 'ducklake_catalog';");
            log.debug("Catalog verification successful");
        }
    }
}
