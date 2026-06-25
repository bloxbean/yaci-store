package com.bloxbean.cardano.yaci.store.analytics.query.service;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * DuckDB-backed transaction analytics queries over Parquet data.
 *
 * <p>SQL is identical to PostgreSQL versions. DuckDB natively supports PERCENTILE_CONT,
 * window functions, CTEs, and all standard aggregates. Hive partition pruning on the
 * date column provides automatic performance optimization for epoch-range queries.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class AnalyticsTransactionQueryService {

    private final AnalyticsQueryExecutor queryExecutor;

    /**
     * Transaction statistics per epoch.
     * Equivalent to MCP tool: epoch-transaction-statistics
     */
    public List<Map<String, Object>> getEpochTransactionStatistics(int startEpoch, int endEpoch) {
        String sql = String.format("""
            SELECT
                epoch,
                COUNT(*) as tx_count,
                COALESCE(SUM(fee), 0) as total_fees,
                COALESCE(AVG(fee), 0) as avg_fee,
                COUNT(DISTINCT block) as block_count,
                SUM(CASE WHEN invalid = false THEN 1 ELSE 0 END) as valid_tx_count,
                SUM(CASE WHEN invalid = true THEN 1 ELSE 0 END) as invalid_tx_count
            FROM transaction
            WHERE epoch BETWEEN %d AND %d
            GROUP BY epoch
            ORDER BY epoch
            """, startEpoch, endEpoch);

        return queryExecutor.queryForList(sql);
    }

    /**
     * Per-block transaction statistics within an epoch range.
     * Equivalent to MCP tool: block-transaction-statistics
     */
    public List<Map<String, Object>> getBlockTransactionStatistics(int startEpoch, int endEpoch, int minTxCount) {
        String sql = String.format("""
            SELECT
                b.number as block_number,
                b.hash as block_hash,
                b.epoch,
                b.slot,
                COUNT(t.tx_hash) as tx_count,
                COALESCE(SUM(t.fee), 0) as total_fees,
                COALESCE(AVG(t.fee), 0) as avg_fee,
                SUM(CASE WHEN t.invalid = false THEN 1 ELSE 0 END) as valid_tx_count,
                SUM(CASE WHEN t.invalid = true THEN 1 ELSE 0 END) as invalid_tx_count
            FROM block b
            LEFT JOIN transaction t ON t.block = b.number
            WHERE b.epoch BETWEEN %d AND %d
            GROUP BY b.number, b.hash, b.epoch, b.slot
            HAVING COUNT(t.tx_hash) >= %d
            ORDER BY b.number DESC
            """, startEpoch, endEpoch, minTxCount);

        return queryExecutor.queryForList(sql);
    }

    /**
     * Fee distribution with percentiles per epoch.
     * Equivalent to MCP tool: fee-distribution-analysis
     */
    public List<Map<String, Object>> getFeeDistributionAnalysis(int startEpoch, int endEpoch) {
        String sql = String.format("""
            SELECT
                epoch,
                COUNT(*) as tx_count,
                COALESCE(SUM(fee), 0) as total_fees,
                COALESCE(MIN(fee), 0) as min_fee,
                COALESCE(MAX(fee), 0) as max_fee,
                COALESCE(AVG(fee), 0) as avg_fee,
                COALESCE(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY fee), 0) as median_fee,
                COALESCE(PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY fee), 0) as p25_fee,
                COALESCE(PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY fee), 0) as p75_fee,
                COALESCE(PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY fee), 0) as p90_fee,
                COALESCE(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY fee), 0) as p95_fee,
                COALESCE(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY fee), 0) as p99_fee
            FROM transaction
            WHERE epoch BETWEEN %d AND %d
              AND invalid = false
            GROUP BY epoch
            ORDER BY epoch
            """, startEpoch, endEpoch);

        return queryExecutor.queryForList(sql);
    }
}
