package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStatus;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.analytics.writer.ExportResult;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Abstract base class for table exporters implementing the template method pattern.
 *
 * This class provides common functionality for all table exporters:
 * - State management (checking if already exported, marking in progress/completed/failed)
 * - Checksum calculation (SHA-256)
 * - Output path construction
 * - Error handling and logging
 *
 * Subclasses only need to implement:
 * - getTableName() - Unique table identifier
 * - getPartitionStrategy() - DAILY or EPOCH
 * - buildQuery() - SQL query for the partition
 *
 * Optional overrides:
 * - buildOutputPath() - Custom output path format
 * - calculateChecksum() - Different checksum algorithm
 *
 * Example:
 * <pre>
 * {@code
 * @Service
 * public class TransactionExporter extends AbstractTableExporter {
 *     @Override
 *     public String getTableName() { return "transactions"; }
 *
 *     @Override
 *     public PartitionStrategy getPartitionStrategy() { return PartitionStrategy.DAILY; }
 *
 *     @Override
 *     protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
 *         return String.format("SELECT * FROM transaction WHERE slot >= %d AND slot < %d",
 *             slotRange.startSlot(), slotRange.endSlot());
 *     }
 * }
 * }
 * </pre>
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractTableExporter implements TableExporter {

    protected final StorageWriter storageWriter;
    protected final ExportStateService stateService;
    protected final EraService eraService;
    protected final AnalyticsStoreProperties properties;

    /**
     * Export data for a specific partition.
     *
     * This template method implements the standard export flow:
     * 1. Check if partition is already completed
     * 2. Mark partition as in progress
     * 3. Build query (calls abstract buildQuery method)
     * 4. Build output path
     * 5. Export to Parquet
     * 6. Calculate checksum
     * 7. Mark as completed or failed
     *
     * @param partition The partition to export
     * @return true if export succeeded, false otherwise
     */
    @Override
    public boolean exportForPartition(PartitionValue partition) {
        String partitionKey = partition.toPathSegment();

        // Check if already completed
        ExportState existingState = stateService.getState(getTableName(), partitionKey);
        if (existingState != null && existingState.getStatus() == ExportStatus.COMPLETED) {
            log.info("Partition {} already exported for table {}, skipping", partitionKey, getTableName());
            return true;
        }

        // Mark in progress
        ExportState state = stateService.markInProgress(getTableName(), partitionKey);
        SlotRange slotRange = partition.toSlotRange(eraService);

        try {
            // Build query (template method - subclass implements)
            String query = buildQuery(partition, slotRange);

            // Build output path
            String outputPath = buildOutputPath(partition);

            // Execute export
            log.info("Exporting {} for partition {} using {}", getTableName(), partitionKey, storageWriter.getStorageFormat());
            ExportResult result = storageWriter.export(query, outputPath);

            // Calculate checksum (skip for DuckLake - files are managed internally)
            String checksum = null;
            if (!"DUCKLAKE".equalsIgnoreCase(storageWriter.getStorageFormat())) {
                checksum = calculateChecksum(outputPath);
            }

            // Mark completed
            stateService.markCompleted(state, result, checksum);
            log.info("Successfully exported {} for partition {}: {} rows, {} bytes",
                    getTableName(), partitionKey, result.getRowCount(), result.getFileSize());

            return true;

        } catch (Exception e) {
            log.error("Failed to export {} for partition {}: {}",
                    getTableName(), partitionKey, e.getMessage(), e);
            stateService.markFailed(state, e.getMessage());
            return false;
        }
    }

    /**
     * Build SQL query for this partition.
     *
     * The query should:
     * - Filter data for the specific slot range
     * - Include ORDER BY clause for optimal Parquet row group organization
     * - Select all columns needed for analytics
     *
     * Example:
     * <pre>
     * return String.format("""
     *     SELECT * FROM transaction
     *     WHERE slot gt;= %d AND slot lt; %d
     *     ORDER BY slot, tx_hash
     *     """, slotRange.startSlot(), slotRange.endSlot());
     * </pre>
     *
     * @param partition The partition being exported
     * @param slotRange The slot range for this partition
     * @return SQL query string
     */
    protected abstract String buildQuery(PartitionValue partition, SlotRange slotRange);

    /**
     * Build output file path for the export.
     *
     * Default format: {export-path}/{table-name}/{partition}/data.parquet
     *
     * Examples:
     * - ./data/analytics/transactions/date=2024-01-15/data.parquet
     * - ./data/analytics/rewards/epoch=450/data.parquet
     *
     * Subclasses can override for custom path formats.
     *
     * @param partition The partition being exported
     * @return Full path to output Parquet file
     */
    protected String buildOutputPath(PartitionValue partition) {
        return Paths.get(
                properties.getExportPath(),
                getTableName(),
                partition.toPathSegment(),
                "data.parquet"
        ).toString();
    }

    /**
     * Calculate SHA-256 checksum of the exported file.
     *
     * Checksums are stored in export_state table and can be used for:
     * - Verifying file integrity
     * - Detecting corruption
     * - Comparing exports across different systems
     *
     * Subclasses can override to use different algorithms (MD5, SHA-512, etc.).
     *
     * @param filePath Path to the exported Parquet file
     * @return Hex-encoded checksum, or null if calculation fails
     */
    protected String calculateChecksum(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = Files.newInputStream(Paths.get(filePath))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to calculate checksum for {}: {}", filePath, e.getMessage());
            return null;
        }
    }
}
