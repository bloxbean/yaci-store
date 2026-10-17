package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

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
    private final DuckLakeWriterLock writerLock;

    public DuckLakeWriterService(@Qualifier("duckDbWriterDataSource") DataSource duckDbDataSource,
                                  AnalyticsStoreProperties properties,
                                  DuckDbConnectionHelper connectionHelper,
                                  DuckLakeWriterLock writerLock) {
        this.duckDbDataSource = duckDbDataSource;
        this.properties = properties;
        this.connectionHelper = connectionHelper;
        this.writerLock = writerLock;

        log.info("Initialized DuckLakeWriterService (catalog: {}, source schema: {}, catalog URL: {})",
                properties.getDucklake().getCatalogType(),
                connectionHelper.getSourceCredentials().getSchema(),
                connectionHelper.getCatalogCredentials().getUrl().equals(connectionHelper.getSourceCredentials().getUrl()) ? "main datasource" : "custom");
    }

    @Override
    public ExportResult export(String query, String outputPath, String partitionColumn) {
        long startTime = System.currentTimeMillis();

        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = duckDbDataSource.getConnection()) {
            // Prepare DuckDB connection for DuckLake (installs extensions, attaches catalog + source, sets search path)
            // readOnly=false for write operations
            connectionHelper.prepareConnectionForDuckLake(conn, true, false);

            // Extract table name from output path
            // Format: ./data/analytics/transactions/date=2024-01-15/data.parquet
            String tableName = extractTableNameFromPath(outputPath);
            Partition partition = partitionOf(outputPath);

            // Handle date partitioning query transformation
            String finalQuery = query;

            if (partition != null && partition.column().equals("date")) {
                // To get "date=yyyy-mm-dd" folder structure, we need an explicit "date" column.
                // It is the exported partition's date, not one derived from each row's timestamp:
                // the export selects rows by slot range, and a row without a block time
                // (COALESCE(block_time, 0)) would otherwise be filed under 1970-01-01.
                finalQuery = String.format("SELECT *, %s AS date FROM (%s)", partition.literal(), query);
            }

            // Create table if it doesn't exist (using schema from query)
            boolean isNewTable = createTableIfNotExists(conn, tableName, finalQuery);

            // Configure partitioning for newly created tables
            // DuckLake requires ALTER TABLE SET PARTITIONED BY after table creation
            if (isNewTable) {
                configurePartitioning(conn, tableName, outputPath, partitionColumn);
            }

            // Replace the partition's rows (DELETE + INSERT in one DuckLake transaction)
            long rowCount = replacePartition(conn, tableName, partition, finalQuery);

            // Note: DuckLake manages its own files with UUID names in partition directories
            // The outputPath is used only for partition detection, not actual file placement
            // File size is not easily calculable as DuckLake may create multiple files
            long fileSize = 0;

            long duration = System.currentTimeMillis() - startTime;

            log.info("DuckLake export completed for table '{}': {} rows, {}ms",
                    tableName, rowCount, duration);

            return new ExportResult(outputPath, rowCount, fileSize, duration);

        } catch (SQLException rawError) {
            // postgres_scanner / PostgreSQL-catalog errors echo the connection string
            // (including the password); log and propagate a redacted copy only.
            SQLException e = DuckDbConnectionHelper.sanitize(rawError);
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
        String schema = connectionHelper.getSourceCredentials().getSchema();
        if (schema == null || schema.isEmpty()) {
            return schema;
        }
        return connectionHelper.quoteIdentifier(schema);
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
     * Replace a partition's rows: DELETE the partition, then INSERT the export, in one DuckLake
     * transaction. A re-export (after an export-state reset, a crash mid-export, or a repair)
     * therefore yields the same rows as the first export instead of appending a second copy.
     * The DELETE covers whole partitions, so DuckLake retires the old data files rather than
     * writing deletion files (which snapshot export refuses).
     */
    private long replacePartition(Connection conn, String tableName, Partition partition, String query)
            throws SQLException {
        if (partition == null) {
            return exportWithDuckLake(conn, tableName, query);
        }

        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(
                    String.format("DELETE FROM %s WHERE %s;", tableName, partition.filter()));
            long inserted = exportWithDuckLake(conn, tableName, query);
            conn.commit();
            if (deleted > 0) {
                log.info("Replaced {} existing rows of DuckLake table '{}' partition {} with {} rows",
                        deleted, tableName, partition.filter(), inserted);
            }
            return inserted;
        } catch (SQLException | RuntimeException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    /**
     * Rows exported before the partition date was authoritative and without a block time
     * (to_timestamp(COALESCE(block_time, 0))) sit in the date=1970-01-01 partition. Once the days
     * holding their slots have been re-exported, those rows exist under their real day as well,
     * and the 1970-01-01 partition is a duplicate.
     *
     * This deletes the whole 1970-01-01 partition only if every row in it has a slot in
     * [startSlot, endSlot) — the slots of the days just re-exported. Otherwise nothing is
     * deleted and the result reports the slots found, so the caller can widen the range.
     *
     * @return what the partition held, and whether it was deleted
     */
    public EpochZeroPartition purgeEpochZeroPartition(String tableName, long startSlot, long endSlot) {
        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = duckDbDataSource.getConnection()) {
            connectionHelper.prepareConnectionForDuckLake(conn, false, false);

            String columnsSql = String.format(
                    "SELECT COUNT(*) FROM duckdb_columns() WHERE database_name = 'ducklake_catalog' "
                            + "AND table_name = '%s' AND column_name IN ('date', 'slot');", tableName);
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(columnsSql)) {
                if (!rs.next() || rs.getInt(1) < 2) {
                    return new EpochZeroPartition(0, null, null, false);
                }
            }

            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                long rows;
                long rowsWithSlot;
                Long minSlot;
                Long maxSlot;
                try (ResultSet rs = stmt.executeQuery(String.format(
                        "SELECT COUNT(*), COUNT(slot), MIN(slot), MAX(slot) FROM %s WHERE %s;",
                        tableName, EPOCH_ZERO_DATE.filter()))) {
                    rs.next();
                    rows = rs.getLong(1);
                    rowsWithSlot = rs.getLong(2);
                    minSlot = rs.getObject(3) != null ? rs.getLong(3) : null;
                    maxSlot = rs.getObject(4) != null ? rs.getLong(4) : null;
                }

                boolean covered = rows > 0 && rowsWithSlot == rows
                        && minSlot >= startSlot && maxSlot < endSlot;
                if (covered) {
                    stmt.executeUpdate(String.format("DELETE FROM %s WHERE %s;", tableName, EPOCH_ZERO_DATE.filter()));
                    log.info("Deleted {} mis-filed rows (slots {}..{}) from DuckLake table '{}' partition date=1970-01-01",
                            rows, minSlot, maxSlot, tableName);
                } else if (rows > 0) {
                    log.warn("DuckLake table '{}' has {} rows under date=1970-01-01 (slots {}..{}) outside the "
                            + "re-exported slots [{}, {}); keeping them", tableName, rows, minSlot, maxSlot,
                            startSlot, endSlot);
                }
                conn.commit();
                return new EpochZeroPartition(rows, minSlot, maxSlot, covered);
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException rawError) {
            SQLException e = DuckDbConnectionHelper.sanitize(rawError);
            log.error("Failed to purge date=1970-01-01 of DuckLake table '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException("DuckLake purge failed: " + e.getMessage(), e);
        }
    }

    /**
     * Rows a DuckLake table holds under date=1970-01-01.
     *
     * @param rowCount rows in the partition
     * @param minSlot  lowest slot among them, or null
     * @param maxSlot  highest slot among them, or null
     * @param deleted  whether the partition was deleted
     */
    public record EpochZeroPartition(long rowCount, Long minSlot, Long maxSlot, boolean deleted) {
    }

    private static final Partition EPOCH_ZERO_DATE = new Partition("date", "DATE '1970-01-01'");

    /** Partition column and its value as a SQL literal. */
    private record Partition(String column, String literal) {
        String filter() {
            return column + " = " + literal;
        }
    }

    /**
     * Partition of an output path, from its Hive-style directory segment
     * (date=2024-01-15 or epoch=450), or null if the path has none.
     */
    private static Partition partitionOf(String outputPath) {
        String segment = Paths.get(outputPath).getParent().getFileName().toString();
        if (segment.startsWith("date=")) {
            return new Partition("date", "DATE '" + LocalDate.parse(segment.substring("date=".length())) + "'");
        }
        if (segment.startsWith("epoch=")) {
            return new Partition("epoch", String.valueOf(Integer.parseInt(segment.substring("epoch=".length()))));
        }
        return null;
    }

    @Override
    public boolean verifyExport(String outputPath, long expectedRowCount) {
        if (expectedRowCount == 0) {
            return true;
        }

        try {
            String tableName = extractTableNameFromPath(outputPath);
            Path outputFilePath = Paths.get(outputPath);
            String partitionSegment = outputFilePath.getParent().getFileName().toString();

            // DuckLake stores files under: {DATA_PATH}/main/{tableName}/{partition}/
            Path duckLakeDir = Paths.get(properties.getExportPath(), "main", tableName, partitionSegment);

            if (!Files.exists(duckLakeDir)) {
                log.warn("DuckLake partition directory does not exist after export: {}", duckLakeDir);
                return false;
            }

            try (var files = Files.list(duckLakeDir)) {
                boolean hasParquet = files.anyMatch(f ->
                        f.getFileName().toString().endsWith(".parquet"));
                if (!hasParquet) {
                    log.warn("No parquet files found in DuckLake directory after export: {}", duckLakeDir);
                }
                return hasParquet;
            }
        } catch (Exception e) {
            log.error("Failed to verify DuckLake export for {}: {}", outputPath, e.getMessage());
            return false;
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
