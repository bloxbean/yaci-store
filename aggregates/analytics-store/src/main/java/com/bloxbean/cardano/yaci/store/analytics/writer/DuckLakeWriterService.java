package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DuckLake-based storage writer implementation.
 *
 * Exports data to Parquet files managed by DuckLake, providing:
 * - ACID transactions
 * - Time-travel queries
 * - Schema evolution
 * - Catalog metadata (PostgreSQL or DuckDB)
 *
 * DuckLake uses:
 * - Parquet files for data storage (same as ParquetWriterService)
 * - Catalog metadata for table management (PostgreSQL or DuckDB)
 * - DuckDB's ducklake extension for catalog operations
 *
 * Example output structure:
 * ./data/analytics/transactions/date=2024-01-15/data.parquet (data file)
 * ducklake_tables, ducklake_snapshots (catalog metadata in PostgreSQL/DuckDB)
 */
@Service("ducklakeWriter")
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeWriterService implements StorageWriter {

    private final DataSource duckDbDataSource;
    private final AnalyticsStoreProperties properties;
    private final DuckDbConnectionHelper connectionHelper;

    public DuckLakeWriterService(@Qualifier("duckDbWriterDataSource") DataSource duckDbDataSource,
                                  AnalyticsStoreProperties properties,
                                  DuckDbConnectionHelper connectionHelper) {
        this.duckDbDataSource = duckDbDataSource;
        this.properties = properties;
        this.connectionHelper = connectionHelper;

        log.info("Initialized DuckLakeWriterService (catalog: {}, source schema: {}, catalog URL: {})",
                properties.getDucklake().getCatalogType(),
                connectionHelper.getSourceCredentials().getSchema(),
                connectionHelper.getCatalogCredentials().getUrl().equals(connectionHelper.getSourceCredentials().getUrl()) ? "main datasource" : "custom");
    }

    @Override
    public ExportResult export(String query, String outputPath) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = duckDbDataSource.getConnection()) {
            // Prepare DuckDB connection for DuckLake (installs extensions, attaches catalog + source, sets search path)
            // readOnly=false for write operations
            connectionHelper.prepareConnectionForDuckLake(conn, true, false);

            // Ensure output directory exists
            Path outputDir = Paths.get(outputPath).getParent();
            if (outputDir != null && !Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Extract table name from output path
            // Format: ./data/analytics/transactions/date=2024-01-15/data.parquet
            String tableName = extractTableNameFromPath(outputPath);

            // Create table if it doesn't exist (using schema from query)
            createTableIfNotExists(conn, tableName, query);

            // Export data using DuckLake INSERT
            long rowCount = exportWithDuckLake(conn, tableName, query);

            // Get file size
            long fileSize = Files.exists(Paths.get(outputPath)) ? Files.size(Paths.get(outputPath)) : 0;

            long duration = System.currentTimeMillis() - startTime;

            log.info("DuckLake export completed: {} ({} rows, {} bytes, {}ms)",
                    outputPath, rowCount, fileSize, duration);

            return new ExportResult(outputPath, rowCount, fileSize, duration);

        } catch (SQLException | IOException e) {
            log.error("Failed to export to DuckLake: {}", e.getMessage(), e);
            throw new RuntimeException("DuckLake export failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageFormat() {
        return "DUCKLAKE";
    }


    /**
     * Create DuckLake table if it doesn't exist.
     * Uses CREATE TABLE AS SELECT with LIMIT 0 to create empty table with correct schema.
     */
    private void createTableIfNotExists(Connection conn, String tableName, String query) throws SQLException {
        // Check if table exists using DuckDB's duckdb_tables()
        String checkSql = String.format(
                "SELECT COUNT(*) FROM duckdb_tables() WHERE database_name = 'ducklake_catalog' AND table_name = '%s';",
                tableName
        );

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.debug("DuckLake table '{}' already exists", tableName);
                return;
            }
        }

        // Create table using CTAS (Create Table As Select) with LIMIT 0 to get schema
        String createTableSql = String.format(
                "CREATE TABLE %s AS %s LIMIT 0;",
                tableName,
                query
        );
        connectionHelper.executeSql(conn, createTableSql);
        log.info("Created DuckLake table: {}", tableName);
    }

    /**
     * Export data using DuckLake INSERT.
     */
    private long exportWithDuckLake(Connection conn, String tableName, String query) throws SQLException {
        // DuckLake manages Parquet settings internally - no need to set compression settings
        // DuckLake uses optimal defaults for Parquet files

        // Insert data into DuckLake table (we're already using ducklake_catalog database)
        String insertSql = String.format(
                "INSERT INTO %s %s;",
                tableName,
                query
        );

        try (Statement stmt = conn.createStatement()) {
            int rowCount = stmt.executeUpdate(insertSql);
            log.debug("Inserted {} rows into DuckLake table '{}'", rowCount, tableName);
            return rowCount;
        }
    }

    /**
     * Extract table name from output path.
     * Format: ./data/analytics/transactions/date=2024-01-15/data.parquet → "transactions"
     */
    private String extractTableNameFromPath(String outputPath) {
        Path path = Paths.get(outputPath);
        // Go up two levels: data.parquet → date=2024-01-15 → transactions
        Path tableDir = path.getParent().getParent();
        return tableDir.getFileName().toString();
    }
}
