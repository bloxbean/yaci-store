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
    public ExportResult export(String query, String outputPath, String partitionColumn) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = duckDbDataSource.getConnection()) {
            // Prepare DuckDB connection for DuckLake (installs extensions, attaches catalog + source, sets search path)
            // readOnly=false for write operations
            connectionHelper.prepareConnectionForDuckLake(conn, true, false);

            // Extract table name from output path
            // Format: ./data/analytics/transactions/date=2024-01-15/data.parquet
            String tableName = extractTableNameFromPath(outputPath);

            // Handle date partitioning query transformation
            boolean isDatePartition = outputPath.contains("date=");
            String finalQuery = query;
            
            if (isDatePartition) {
                // To get "date=yyyy-mm-dd" folder structure, we need an explicit "date" column
                finalQuery = String.format("SELECT *, CAST(timezone('UTC', %s) AS DATE) AS date FROM (%s)", 
                        partitionColumn, query);
            }

            // Create table if it doesn't exist (using schema from query)
            boolean isNewTable = createTableIfNotExists(conn, tableName, finalQuery);

            // Configure partitioning for newly created tables
            // DuckLake requires ALTER TABLE SET PARTITIONED BY after table creation
            if (isNewTable) {
                configurePartitioning(conn, tableName, outputPath, partitionColumn);
            }

            // Export data using DuckLake INSERT
            long rowCount = exportWithDuckLake(conn, tableName, finalQuery);

            // Note: DuckLake manages its own files with UUID names in partition directories
            // The outputPath is used only for partition detection, not actual file placement
            // File size is not easily calculable as DuckLake may create multiple files
            long fileSize = 0;

            long duration = System.currentTimeMillis() - startTime;

            log.info("DuckLake export completed for table '{}': {} rows, {}ms",
                    tableName, rowCount, duration);

            return new ExportResult(outputPath, rowCount, fileSize, duration);

        } catch (SQLException e) {
            log.error("Failed to export to DuckLake: {}", e.getMessage(), e);
            throw new RuntimeException("DuckLake export failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageFormat() {
        return "DUCKLAKE";
    }

    @Override
    public String getSourceSchema() {
        return connectionHelper.getSourceCredentials().getSchema();
    }

    /**
     * Create DuckLake table if it doesn't exist.
     * Uses CREATE TABLE AS SELECT with LIMIT 0 to create empty table with correct schema.
     *
     * @return true if table was created, false if it already existed
     */
    private boolean createTableIfNotExists(Connection conn, String tableName, String query) throws SQLException {
        // Check if table exists using DuckDB's duckdb_tables()
        String checkSql = String.format(
                "SELECT COUNT(*) FROM duckdb_tables() WHERE database_name = 'ducklake_catalog' AND table_name = '%s';",
                tableName
        );

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                log.debug("DuckLake table '{}' already exists", tableName);
                return false;  // Table already exists
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
    return true;  // Table was newly created
    }

    /**
     * Configure partitioning for a DuckLake table based on the output path format and partition column.
     *
     * DuckLake requires ALTER TABLE SET PARTITIONED BY after table creation.
     * Partition configuration is derived from:
     * - Path format: "date=" → year/month/day transforms, "epoch=" → identity transform
     * - Partition column: Provided by the exporter (e.g., "block_time", "spent_block_time")
     *
     * Note: Timestamp columns are cast to TIMESTAMP type in the exporters (via to_timestamp()).
     *
     * DuckLake will create Hive-style partitions like:
     * - main/transactions/year=2024/month=1/day=15/ducklake-{uuid}.parquet
     * - main/spent_outputs/spentyear=2024/month=1/day=15/ducklake-{uuid}.parquet
     * - main/rewards/epoch=450/ducklake-{uuid}.parquet
     *
     * @param conn DuckDB connection
     * @param tableName Table name to configure
     * @param outputPath Output path containing partition format (date= or epoch=)
     * @param partitionColumn Column name for time-based partitioning (e.g., "block_time", "spent_block_time")
     */
    private void configurePartitioning(Connection conn, String tableName, String outputPath, String partitionColumn) throws SQLException {
        String alterSql = null;
        String partitionDesc = null;

        // Extract partition type from path format
        if (outputPath.contains("date=")) {
            // Single-level date partitioning: date=yyyy-mm-dd
            // Uses the injected "date" column
            alterSql = String.format(
                    "ALTER TABLE %s SET PARTITIONED BY (date);",
                    tableName
            );
            partitionDesc = "date";
        } else if (outputPath.contains("epoch=")) {
            // Epoch-based partitioning: Use epoch column directly (identity transform)
            // Result: epoch=450 partitions
            alterSql = String.format(
                    "ALTER TABLE %s SET PARTITIONED BY (epoch);",
                    tableName
            );
            partitionDesc = "epoch";
        }

        // Execute partition configuration if applicable
        if (alterSql != null) {
            connectionHelper.executeSql(conn, alterSql);
            log.info("Configured DuckLake table '{}' with {} partitioning", tableName, partitionDesc);
        } else {
            log.warn("Could not determine partition type from path: {}", outputPath);
        }
    }

    /**
     * Export data using DuckLake INSERT.
     */
    private long exportWithDuckLake(Connection conn, String tableName, String query) throws SQLException {
        // Compression settings are configured globally at catalog level
        // See DuckLakeCatalogInitializer which calls configureDuckLakeCatalogSettings()
        // Settings are applied to all Parquet files written by DuckLake

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
