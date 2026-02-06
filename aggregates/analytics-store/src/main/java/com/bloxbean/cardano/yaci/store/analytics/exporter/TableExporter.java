package com.bloxbean.cardano.yaci.store.analytics.exporter;

/**
 * Interface for table exporters that export blockchain data to Parquet files.
 *
 * Implementations export specific tables (e.g., transactions, address_balance, rewards)
 * using a defined partition strategy (daily, epoch-based, etc.).
 *
 * The framework uses Spring auto-discovery to find all {@code @Service} implementations
 * and registers them in {@link TableExporterRegistry}.
 *
 * Example implementation:
 * <pre>
 * {@code
 * @Service
 * public class TransactionExporter extends AbstractTableExporter {
 *     @Override
 *     public String getTableName() {
 *         return "transactions";
 *     }
 *
 *     @Override
 *     public PartitionStrategy getPartitionStrategy() {
 *         return PartitionStrategy.DAILY;
 *     }
 *
 *     @Override
 *     protected String buildQuery(PartitionValue partition, SlotRange slotRange) {
 *         return "SELECT * FROM transaction WHERE slot >= " + slotRange.startSlot()
 *                + " AND slot < " + slotRange.endSlot();
 *     }
 * }
 * }
 * </pre>
 */
public interface TableExporter {

    /**
     * Get the unique table name.
     *
     * This name is used for:
     * - State tracking in export_state table
     * - Output directory path (e.g., {export-path}/{table-name}/date=2024-01-15/)
     * - Configuration (enabling/disabling specific tables)
     *
     * @return Unique table identifier (e.g., "transactions", "address_balance")
     */
    String getTableName();

    /**
     * Get the partition strategy for this table.
     *
     * Determines how data is partitioned:
     * - DAILY: Date-based partitions (transaction data, address balances)
     * - EPOCH: Epoch-based partitions (rewards, stake snapshots)
     * - MONTHLY: Monthly partitions (future use for aggregates)
     *
     * @return Partition strategy for this table
     */
    PartitionStrategy getPartitionStrategy();

    /**
     * Get the timestamp column name used for partitioning.
     *
     * This column is used by DuckLake to configure time-based partitioning.
     * Most tables use "block_time", but some (like spent_outputs) use different
     * timestamp columns (e.g., "spent_block_time").
     *
     * Examples:
     * - transactions → "block_time"
     * - transaction_outputs → "block_time"
     * - address_balance → "block_time"
     * - spent_outputs → "spent_block_time"
     *
     * @return Column name for partitioning (e.g., "block_time", "spent_block_time")
     */
    String getPartitionColumn();

    /**
     * Export data for a specific partition.
     *
     * Implementations should:
     * 1. Check if partition is already exported (via ExportStateService)
     * 2. Build SQL query for the partition
     * 3. Export to Parquet using DuckDbWriterService
     * 4. Update state (COMPLETED or FAILED)
     *
     * The {@link AbstractTableExporter} base class provides a standard implementation
     * that handles state management, checksum calculation, and error handling.
     *
     * @param partition The partition to export (date or epoch)
     * @return true if export succeeded, false otherwise
     */
    boolean exportForPartition(PartitionValue partition);

    /**
     * Pre-export validation hook to check if prerequisites are met before export.
     *
     * This method is called before the actual export process begins. Exporters can
     * override this method to implement custom validation logic, such as:
     * - Checking if dependent jobs have completed (e.g., AdaPot job for reward tables)
     * - Verifying data availability
     * - Validating partition readiness
     *
     * Default implementation returns true (no validation required).
     *
     * Example usage:
     * <pre>
     * {@code
     * @Override
     * protected boolean preExportValidation(PartitionValue partition) {
     *     int epoch = ((PartitionValue.EpochPartition) partition).epoch();
     *     return isRewardCalcAdaPotJobCompleted(epoch);
     * }
     * }
     * </pre>
     *
     * @param partition The partition to be exported
     * @return true if validation passes and export can proceed, false to skip export
     */
    default boolean preExportValidation(PartitionValue partition) {
        return true;
    }
}
