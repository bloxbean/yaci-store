package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.mcp.server.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service providing stake pool aggregation analytics.
 * Focuses on delegation and reward statistics to complement existing block production metrics.
 *
 * Key Features:
 * - Pool delegator analysis
 * - Pool reward distribution statistics
 * - Pool ranking and comparison
 * - Combined pool performance metrics
 *
 * Use Cases:
 * - Delegator decision support
 * - Pool operator performance tracking
 * - Pool discovery and comparison
 * - ROI and reward analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.staking.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpPoolAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "pool-delegator-stats",
          description = "Get delegation statistics for a specific pool from epoch stake snapshots. " +
                        "Returns unique delegator count and total staked amount. " +
                        "Uses official protocol snapshots (epoch_stake table). " +
                        "Epoch parameter refers to active_epoch (when stake is active for block production). " +
                        "Pool ID can be bech32 (pool1...) or hex format.")
    public PoolDelegatorStats getPoolDelegatorStats(
        @ToolParam(description = "Pool ID (bech32 pool1... or hex)") String poolId,
        @ToolParam(description = "Active epoch (when stake is active, null for latest)") Integer epoch
    ) {
        log.debug("Getting delegator stats for pool: {}, active epoch: {}", poolId, epoch);

        // Convert bech32 to hex if needed
        String poolIdHex = poolId.startsWith("pool1")
            ? PoolUtil.getPoolHash(poolId)
            : poolId;

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolIdHex);

        if (epoch != null) {
            sql = """
                SELECT
                    pool_id,
                    COUNT(DISTINCT address) as delegator_count,
                    COALESCE(SUM(amount), 0) as total_stake
                FROM epoch_stake
                WHERE pool_id = :poolId
                  AND active_epoch = :epoch
                GROUP BY pool_id
                """;
            params.put("epoch", epoch);
        } else {
            // Use latest active_epoch
            sql = """
                SELECT
                    pool_id,
                    COUNT(DISTINCT address) as delegator_count,
                    COALESCE(SUM(amount), 0) as total_stake
                FROM epoch_stake
                WHERE pool_id = :poolId
                  AND active_epoch = (SELECT MAX(active_epoch) FROM epoch_stake)
                GROUP BY pool_id
                """;
        }

        List<PoolDelegatorStats> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                return new PoolDelegatorStats(
                    poolIdHex,
                    poolIdBech32,
                    rs.getInt("delegator_count"),
                    rs.getBigDecimal("total_stake").toBigInteger(),
                    epoch
                );
            }
        );

        if (results.isEmpty()) {
            // Return zero stats if no stake found
            String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
            return new PoolDelegatorStats(poolIdHex, poolIdBech32, 0, BigInteger.ZERO, epoch);
        }

        return results.get(0);
    }

    @Tool(name = "pools-ranking-by-delegators",
          description = "Get pools ranked by delegator count from epoch stake snapshots. " +
                        "Returns top pools ordered by number of unique delegators. " +
                        "Uses official protocol snapshots (epoch_stake table). " +
                        "Epoch parameter refers to active_epoch (when stake is active). " +
                        "Useful for discovering popular pools and analyzing pool size distribution.")
    public List<PoolRanking> getPoolsRankingByDelegators(
        @ToolParam(description = "Minimum delegator count (default: 1)") Integer minDelegators,
        @ToolParam(description = "Active epoch (null for latest)") Integer epoch,
        @ToolParam(description = "Maximum results (default: 50, max: 200)") Integer limit
    ) {
        log.debug("Getting pool rankings by delegators: minDelegators={}, epoch={}, limit={}",
                  minDelegators, epoch, limit);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;
        int effectiveMinDelegators = (minDelegators != null && minDelegators > 0) ? minDelegators : 1;

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("minDelegators", effectiveMinDelegators);
        params.put("limit", effectiveLimit);

        if (epoch != null) {
            sql = """
                WITH pool_stats AS (
                    SELECT
                        pool_id,
                        COUNT(DISTINCT address) as delegator_count,
                        SUM(amount) as total_stake
                    FROM epoch_stake
                    WHERE active_epoch = :epoch
                    GROUP BY pool_id
                    HAVING COUNT(DISTINCT address) >= :minDelegators
                )
                SELECT
                    pool_id,
                    delegator_count,
                    ROW_NUMBER() OVER (ORDER BY delegator_count DESC, total_stake DESC) as rank
                FROM pool_stats
                ORDER BY rank
                LIMIT :limit
                """;
            params.put("epoch", epoch);
        } else {
            // Latest active epoch
            sql = """
                WITH pool_stats AS (
                    SELECT
                        pool_id,
                        COUNT(DISTINCT address) as delegator_count,
                        SUM(amount) as total_stake
                    FROM epoch_stake
                    WHERE active_epoch = (SELECT MAX(active_epoch) FROM epoch_stake)
                    GROUP BY pool_id
                    HAVING COUNT(DISTINCT address) >= :minDelegators
                )
                SELECT
                    pool_id,
                    delegator_count,
                    ROW_NUMBER() OVER (ORDER BY delegator_count DESC, total_stake DESC) as rank
                FROM pool_stats
                ORDER BY rank
                LIMIT :limit
                """;
        }

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdHex = rs.getString("pool_id");
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                return new PoolRanking(
                    poolIdHex,
                    poolIdBech32,
                    rs.getInt("delegator_count"),
                    rs.getInt("rank"),
                    epoch
                );
            }
        );
    }

    @Tool(name = "pool-rewards-summary",
          description = "Get reward distribution statistics for a specific pool. " +
                        "Returns total rewards distributed, recipient count, and averages. " +
                        "Critical metric for delegators evaluating pool performance. " +
                        "Supports filtering by epoch range. " +
                        "Pool ID can be in bech32 (pool1...) or hex format.")
    public PoolRewardStats getPoolRewardsSummary(
        @ToolParam(description = "Pool ID (bech32 or hex)") String poolId,
        @ToolParam(description = "Start epoch (null for all-time)") Integer startEpoch,
        @ToolParam(description = "End epoch (null for all-time)") Integer endEpoch
    ) {
        log.debug("Getting reward summary for pool: {}, epochs: {}-{}",
                  poolId, startEpoch, endEpoch);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolId);

        if (startEpoch != null && endEpoch != null) {
            sql = """
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(*) as reward_event_count,
                    COUNT(DISTINCT address) as unique_recipients
                FROM reward
                WHERE pool_id = :poolId
                  AND earned_epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY pool_id
                """;
            params.put("startEpoch", startEpoch);
            params.put("endEpoch", endEpoch);
        } else {
            sql = """
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(*) as reward_event_count,
                    COUNT(DISTINCT address) as unique_recipients
                FROM reward
                WHERE pool_id = :poolId
                GROUP BY pool_id
                """;
        }

        List<PoolRewardStats> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdHex = rs.getString("pool_id");
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                BigDecimal totalRewards = rs.getBigDecimal("total_rewards");
                int uniqueRecipients = rs.getInt("unique_recipients");
                BigDecimal avgReward = uniqueRecipients > 0
                    ? totalRewards.divide(BigDecimal.valueOf(uniqueRecipients), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new PoolRewardStats(
                    poolIdHex,
                    poolIdBech32,
                    totalRewards,
                    rs.getInt("reward_event_count"),
                    uniqueRecipients,
                    avgReward,
                    startEpoch,
                    endEpoch
                );
            }
        );

        if (results.isEmpty()) {
            // Return zero stats if no rewards found
            String poolIdBech32 = PoolUtil.getBech32PoolId(poolId);
            return new PoolRewardStats(poolId, poolIdBech32, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, startEpoch, endEpoch);
        }

        return results.get(0);
    }

    @Tool(name = "top-reward-pools",
          description = "Get pools ranked by total rewards distributed. " +
                        "Returns top pools ordered by reward amount. " +
                        "Useful for finding highest-performing pools and ROI analysis. " +
                        "Supports filtering by epoch range. " +
                        "Limit controls maximum results (default: 50, max: 200).")
    public List<PoolRewardStats> getTopRewardPools(
        @ToolParam(description = "Start epoch (null for all-time)") Integer startEpoch,
        @ToolParam(description = "End epoch (null for all-time)") Integer endEpoch,
        @ToolParam(description = "Maximum results (default: 50, max: 200)") Integer limit
    ) {
        log.debug("Getting top reward pools: epochs: {}-{}, limit: {}",
                  startEpoch, endEpoch, limit);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("limit", effectiveLimit);

        if (startEpoch != null && endEpoch != null) {
            sql = """
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(*) as reward_event_count,
                    COUNT(DISTINCT address) as unique_recipients
                FROM reward
                WHERE earned_epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY pool_id
                ORDER BY total_rewards DESC
                LIMIT :limit
                """;
            params.put("startEpoch", startEpoch);
            params.put("endEpoch", endEpoch);
        } else {
            sql = """
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(*) as reward_event_count,
                    COUNT(DISTINCT address) as unique_recipients
                FROM reward
                GROUP BY pool_id
                ORDER BY total_rewards DESC
                LIMIT :limit
                """;
        }

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdHex = rs.getString("pool_id");
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                BigDecimal totalRewards = rs.getBigDecimal("total_rewards");
                int uniqueRecipients = rs.getInt("unique_recipients");
                BigDecimal avgReward = uniqueRecipients > 0
                    ? totalRewards.divide(BigDecimal.valueOf(uniqueRecipients), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new PoolRewardStats(
                    poolIdHex,
                    poolIdBech32,
                    totalRewards,
                    rs.getInt("reward_event_count"),
                    uniqueRecipients,
                    avgReward,
                    startEpoch,
                    endEpoch
                );
            }
        );
    }

    @Tool(name = "pool-complete-stats",
          description = "Get comprehensive statistics combining blocks, delegations, and rewards for a pool. " +
                        "Returns complete performance picture for pool analysis. " +
                        "Essential for thorough pool evaluation and comparison. " +
                        "Combines data from block, epoch_stake, and reward tables. " +
                        "Pool ID can be in bech32 (pool1...) or hex format.")
    public PoolCompleteStats getPoolCompleteStats(
        @ToolParam(description = "Pool ID (bech32 or hex)") String poolId,
        @ToolParam(description = "Epoch number for analysis") int epoch
    ) {
        log.debug("Getting complete stats for pool: {}, epoch: {}", poolId, epoch);

        // Convert bech32 to hex if needed
        String poolIdHex = poolId.startsWith("pool1")
            ? PoolUtil.getPoolHash(poolId)
            : poolId;

        String sql = """
            WITH block_stats AS (
                SELECT
                    slot_leader as pool_id,
                    COUNT(*) as blocks_produced,
                    COALESCE(SUM(no_of_txs), 0) as total_transactions,
                    COALESCE(SUM(total_fees), 0) as total_fees
                FROM block
                WHERE slot_leader = :poolId
                  AND epoch = :epoch
                GROUP BY slot_leader
            ),
            delegation_stats AS (
                SELECT
                    pool_id,
                    COUNT(DISTINCT address) as delegator_count,
                    COALESCE(SUM(amount), 0) as total_stake
                FROM epoch_stake
                WHERE pool_id = :poolId
                  AND active_epoch = :epoch
                GROUP BY pool_id
            ),
            reward_stats AS (
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(DISTINCT address) as reward_recipients
                FROM reward
                WHERE pool_id = :poolId
                  AND earned_epoch = :epoch
                GROUP BY pool_id
            )
            SELECT
                :poolId as pool_id,
                COALESCE(bs.blocks_produced, 0) as blocks_produced,
                COALESCE(bs.total_transactions, 0) as total_transactions,
                COALESCE(bs.total_fees, 0) as total_fees,
                COALESCE(ds.delegator_count, 0) as delegator_count,
                COALESCE(ds.total_stake, 0) as total_stake,
                COALESCE(rs.total_rewards, 0) as total_rewards,
                COALESCE(rs.reward_recipients, 0) as reward_recipients
            FROM (SELECT 1) as dummy
            LEFT JOIN block_stats bs ON true
            LEFT JOIN delegation_stats ds ON true
            LEFT JOIN reward_stats rs ON true
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolIdHex);
        params.put("epoch", epoch);

        String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);

        List<PoolCompleteStats> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PoolCompleteStats(
                poolIdHex,
                poolIdBech32,
                epoch,
                rs.getInt("blocks_produced"),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("total_fees"),
                rs.getInt("delegator_count"),
                rs.getBigDecimal("total_stake").toBigInteger(),
                rs.getBigDecimal("total_rewards"),
                rs.getInt("reward_recipients")
            )
        );

        if (results.isEmpty()) {
            // Return zero stats if pool has no activity
            return new PoolCompleteStats(poolIdHex, poolIdBech32, epoch, 0, 0L, BigDecimal.ZERO, 0, BigInteger.ZERO, BigDecimal.ZERO, 0);
        }

        return results.get(0);
    }
}
