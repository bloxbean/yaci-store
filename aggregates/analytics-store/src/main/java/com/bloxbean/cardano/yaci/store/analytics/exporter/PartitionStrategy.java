package com.bloxbean.cardano.yaci.store.analytics.exporter;

/**
 * Defines the partitioning strategy for table exports.
 *
 * Different blockchain data has different natural partitioning:
 * - Transaction data changes frequently -> Daily partitions
 * - Epoch-specific data (rewards, stake snapshots) -> Epoch partitions
 * - Monthly aggregates for long-term storage -> Monthly partitions (future)
 */
public enum PartitionStrategy {
    /**
     * Partition by date (date=yyyy-MM-dd).
     * Best for transaction data and address balances.
     *
     * Examples:
     * - transaction_outputs/date=2024-01-15/
     * - address_balance/date=2024-01-15/
     */
    DAILY,

    /**
     * Partition by epoch (epoch=N).
     * Best for epoch-specific data like rewards and stake snapshots.
     *
     * Examples:
     * - rewards/epoch=450/
     * - epoch_stake/epoch=450/
     */
    EPOCH,

    /**
     * Partition by month (year=yyyy/month=MM).
     * Reserved for future use with monthly aggregates.
     *
     * Examples:
     * - aggregated_metrics/year=2024/month=01/
     */
    MONTHLY
}
