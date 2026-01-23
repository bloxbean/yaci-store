package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Parquet storage writer using DuckDB's postgres_scanner extension.
 *
 * This implementation exports PostgreSQL data directly to Parquet files without
 * using a lakehouse catalog. Files are organized in Hive-style partitions.
 *
 * Output structure:
 * - {export-path}/transactions/date=2024-01-15/data.parquet
 * - {export-path}/transaction_outputs/date=2024-01-15/data.parquet
 *
 * Features:
 * - Direct Parquet export via DuckDB COPY command
 * - Configurable compression (ZSTD, SNAPPY, GZIP, etc.)
 * - Connection pooling for performance
 * - Hive-style partitioning (compatible with Spark, Presto, etc.)
 */
@Service("parquetWriter")
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "parquet", matchIfMissing = true)
public class ParquetWriterService implements StorageWriter {

    private final DataSource duckDbDataSource;
    private final AnalyticsStoreProperties properties;
    private final DuckDbConnectionHelper connectionHelper;

    public ParquetWriterService(@Qualifier("duckDbWriterDataSource") DataSource duckDbDataSource,
                                AnalyticsStoreProperties properties,
                                DuckDbConnectionHelper connectionHelper) {
        this.duckDbDataSource = duckDbDataSource;
        this.properties = properties;
        this.connectionHelper = connectionHelper;
        log.info("Initialized ParquetWriterService (direct Parquet export)");
    }

    @Override
    public ExportResult export(String query, String outputPath, String partitionColumn) {
        long startTime = System.currentTimeMillis();

        // Note: Parquet mode doesn't use partitionColumn - it writes directly to the specified path
        // The partition column is only needed for DuckLake's ALTER TABLE SET PARTITIONED BY

        try {
            // Prepare file paths
            Path outputFile = Paths.get(outputPath);
            Path tempFile = Paths.get(outputPath + ".tmp");
            Path targetDirectory = outputFile.getParent();

            // Ensure output directory exists
            Files.createDirectories(targetDirectory);

            // Clean up any orphaned .tmp files from previous failed writes
            cleanupTempFiles(targetDirectory);

            // Get DuckDB connection from pool
            try (Connection duckConn = duckDbDataSource.getConnection();
                 Statement stmt = duckConn.createStatement()) {

                // Setup PostgreSQL connection using helper
                if (!connectionHelper.isDatabaseAttached(duckConn, "source_db")) {
                    log.debug("PostgreSQL not attached, setting up connection");
                    connectionHelper.installPostgresScanner(duckConn);
                    connectionHelper.attachSourceDatabase(duckConn, "source_db");

                    // Set schema context so unqualified table names resolve correctly
                    String pgSchema = connectionHelper.getSourceCredentials().getSchema();
                    if (pgSchema != null && !pgSchema.isEmpty()) {
                        stmt.execute(String.format("SET schema 'source_db.%s'", pgSchema));
                        log.debug("Set DuckDB schema context to: source_db.{}", pgSchema);
                    }
                } else {
                    log.debug("PostgreSQL already attached, reusing connection");
                }

                // Export to Parquet with configurable compression
                String codec = properties.getParquetExport().getCodec();
                int compressionLevel = properties.getParquetExport().getCompressionLevel();
                int rowGroupSize = properties.getParquetExport().getRowGroupSize();

                // Build COPY command to write to temporary file (.tmp)
                String tempPath = tempFile.toString();
                String exportCmd;
                if ("ZSTD".equalsIgnoreCase(codec)) {
                    // ZSTD supports compression level
                    if (rowGroupSize > 0) {
                        exportCmd = String.format(
                            "COPY (%s) TO '%s' (FORMAT PARQUET, CODEC '%s', COMPRESSION_LEVEL %d, ROW_GROUP_SIZE %d)",
                            query, tempPath, codec.toUpperCase(), compressionLevel, rowGroupSize
                        );
                    } else {
                        exportCmd = String.format(
                            "COPY (%s) TO '%s' (FORMAT PARQUET, CODEC '%s', COMPRESSION_LEVEL %d)",
                            query, tempPath, codec.toUpperCase(), compressionLevel
                        );
                    }
                } else {
                    // Other codecs don't support compression level
                    if (rowGroupSize > 0) {
                        exportCmd = String.format(
                            "COPY (%s) TO '%s' (FORMAT PARQUET, COMPRESSION '%s', ROW_GROUP_SIZE %d)",
                            query, tempPath, codec.toUpperCase(), rowGroupSize
                        );
                    } else {
                        exportCmd = String.format(
                            "COPY (%s) TO '%s' (FORMAT PARQUET, COMPRESSION '%s')",
                            query, tempPath, codec.toUpperCase()
                        );
                    }
                }

                log.debug("Exporting with codec: {}, level: {}, rowGroupSize: {}",
                    codec, compressionLevel, rowGroupSize > 0 ? rowGroupSize : "default");

                // Execute COPY to temporary file
                stmt.execute(exportCmd);

                // Get row count and file size before rename
                long rowCount = getRowCount(stmt, query);
                long fileSize = Files.size(tempFile);

                // Atomically rename temporary file to final destination
                // This makes the file visible to readers only after it's complete
                try {
                    Files.move(tempFile, outputFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Atomically renamed {} to {}", tempFile.getFileName(), outputFile.getFileName());
                } catch (AtomicMoveNotSupportedException e) {
                    // Fallback to non-atomic move if filesystem doesn't support atomic moves
                    log.warn("Atomic move not supported, using standard move for {}", outputFile);
                    Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
                }

                long duration = System.currentTimeMillis() - startTime;

                log.info("Successfully exported {} rows to {} ({} bytes) in {} ms using {}",
                    rowCount, outputPath, fileSize, duration, getStorageFormat());

                return new ExportResult(outputPath, rowCount, fileSize, duration);
            }
        } catch (Exception e) {
            log.error("Failed to export to Parquet: {}", e.getMessage(), e);
            throw new RuntimeException("Export to Parquet failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getStorageFormat() {
        return "PARQUET";
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
     * Get row count from a query by wrapping it in COUNT(*)
     */
    private long getRowCount(Statement stmt, String query) throws SQLException {
        String countQuery = String.format("SELECT COUNT(*) FROM (%s) AS count_query", query);
        var rs = stmt.executeQuery(countQuery);
        if (rs.next()) {
            return rs.getLong(1);
        }
        return 0;
    }

    /**
     * Clean up orphaned .tmp files in the target directory.
     *
     * Removes any *.tmp files that were left behind from previous failed/crashed writes.
     * This prevents accumulation of incomplete files and ensures a clean state before writing.
     *
     * @param targetDirectory The directory to clean
     */
    private void cleanupTempFiles(Path targetDirectory) {
        if (!Files.exists(targetDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDirectory, "*.tmp")) {
            for (Path tempFile : stream) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("Cleaned up orphaned temp file: {}", tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file {}: {}", tempFile, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup temp files in {}: {}", targetDirectory, e.getMessage());
        }
    }
}
