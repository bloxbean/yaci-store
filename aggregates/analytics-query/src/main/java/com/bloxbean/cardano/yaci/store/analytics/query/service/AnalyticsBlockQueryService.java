package com.bloxbean.cardano.yaci.store.analytics.query.service;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * DuckDB-backed block analytics queries over Parquet data.
 *
 * <p>These queries are identical to the PostgreSQL versions because the block table
 * schema is unchanged in Parquet. DuckDB handles CTEs, GROUP BY, and aggregate
 * functions natively with automatic Hive partition pruning on the date column.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class AnalyticsBlockQueryService {

    private final AnalyticsQueryExecutor queryExecutor;

    /**
     * Pool block production statistics per epoch.
     * Equivalent to MCP tool: pool-block-production-stats
     */
    public List<Map<String, Object>> getPoolBlockProductionStats(String poolId, int startEpoch, int endEpoch) {
        String sql = String.format("""
            SELECT
                epoch,
                COUNT(*) as blocks_produced,
                COALESCE(SUM(no_of_txs), 0) as total_transactions,
                COALESCE(SUM(total_fees), 0) as total_fees,
                COALESCE(AVG(no_of_txs), 0) as avg_txs_per_block
            FROM block
            WHERE slot_leader = '%s'
              AND epoch BETWEEN %d AND %d
            GROUP BY epoch
            ORDER BY epoch
            """, escapeSql(poolId), startEpoch, endEpoch);

        return queryExecutor.queryForList(sql);
    }

    /**
     * Network-wide block statistics per epoch.
     * Equivalent to MCP tool: epoch-block-statistics
     */
    public List<Map<String, Object>> getEpochBlockStatistics(int startEpoch, int endEpoch) {
        String sql = String.format("""
            SELECT
                epoch,
                COUNT(*) as block_count,
                COALESCE(SUM(no_of_txs), 0) as total_transactions,
                COALESCE(SUM(total_fees), 0) as total_fees,
                COALESCE(AVG(no_of_txs), 0) as avg_txs_per_block,
                COALESCE(AVG(body_size), 0) as avg_block_size,
                COUNT(DISTINCT slot_leader) as unique_pool_count
            FROM block
            WHERE epoch BETWEEN %d AND %d
            GROUP BY epoch
            ORDER BY epoch
            """, startEpoch, endEpoch);

        return queryExecutor.queryForList(sql);
    }

    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }
}
