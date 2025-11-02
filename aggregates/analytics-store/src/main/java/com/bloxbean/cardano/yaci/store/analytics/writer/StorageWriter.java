package com.bloxbean.cardano.yaci.store.analytics.writer;

/**
 * Storage abstraction for exporting blockchain data.
 *
 * This interface abstracts the underlying storage format, allowing implementations
 * to use either direct Parquet files or lakehouse formats like DuckLake.
 *
 * Implementations:
 * - {@link ParquetWriterService} - Direct Parquet export using DuckDB
 * - {@link DuckLakeWriterService} - DuckLake format with PostgreSQL/DuckDB catalog
 *
 * The storage layer is swappable without modifying table exporters, thanks to
 * the Interface + Registry pattern established in Phase 1.
 */
public interface StorageWriter {

    /**
     * Export data from a PostgreSQL table/query to storage.
     *
     * The implementation determines the actual storage format:
     * - Parquet: Direct Parquet files in Hive-style partitions
     * - DuckLake: Parquet files managed by DuckLake catalog
     *
     * @param query SQL query to execute (can include WHERE clause for partitioning)
     * @param outputPath Path where the data will be written (format depends on implementation)
     * @return ExportResult containing export statistics
     * @throws RuntimeException if export fails
     */
    ExportResult export(String query, String outputPath);

    /**
     * Get the storage format name for logging/monitoring.
     *
     * @return Storage format identifier (e.g., "PARQUET", "DUCKLAKE")
     */
    String getStorageFormat();
}
