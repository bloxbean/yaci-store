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

    //New Pool tools

    @Tool(name = "pool-saturation-check",
          description = "Check if a pool is approaching saturation or oversaturated. " +
                        "Returns pool stake, saturation percentage, and saturation level. " +
                        "CRITICAL for delegators: oversaturated pools (>100%) provide diminishing returns. " +
                        "Saturation is calculated as: (pool_stake / (total_network_stake / nOpt)) × 100. " +
                        "Pool ID can be bech32 (pool1...) or hex format. " +
                        "If epoch is null, uses latest available epoch.")
    public PoolSaturationInfo getPoolSaturationCheck(
        @ToolParam(description = "Pool ID (bech32 pool1... or hex)") String poolId,
        @ToolParam(description = "Active epoch (null for latest)") Integer epoch
    ) {
        log.debug("Checking saturation for pool: {}, epoch: {}", poolId, epoch);

        // Convert bech32 to hex if needed
        String poolIdHex = poolId.startsWith("pool1")
            ? PoolUtil.getPoolHash(poolId)
            : poolId;

        String sql = """
            WITH latest_epoch AS (
                SELECT
                    LEAST(
                        (SELECT MAX(active_epoch) FROM epoch_stake),
                        (SELECT MAX(epoch) FROM adapot)
                    ) as epoch
            ),
            pool_stake AS (
                SELECT
                    ps.pool_id,
                    ps.active_epoch,
                    SUM(ps.amount) as total_stake,
                    COUNT(DISTINCT ps.address) as delegator_count
                FROM epoch_stake ps
                WHERE ps.pool_id = :poolId
                  AND ps.active_epoch = COALESCE(:epoch, (SELECT epoch FROM latest_epoch))
                GROUP BY ps.pool_id, ps.active_epoch
            ),
            circulation_supply AS (
                SELECT
                    ap.epoch,
                    ap.circulation as total_circulation
                FROM adapot ap
                WHERE ap.epoch = COALESCE(:epoch, (SELECT epoch FROM latest_epoch))
            ),
            epoch_params AS (
                SELECT
                    ep.epoch,
                    (ep.params->>'nopt')::integer as nopt
                FROM epoch_param ep
                WHERE ep.epoch = (SELECT MAX(epoch) FROM epoch_param WHERE epoch <= COALESCE(:epoch, (SELECT epoch FROM latest_epoch)))
            )
            SELECT
                ps.pool_id,
                ps.active_epoch as epoch,
                ps.total_stake,
                ps.delegator_count,
                cs.total_circulation,
                ep.nopt,
                (cs.total_circulation / ep.nopt) as optimal_stake_threshold,
                ROUND((ps.total_stake::numeric / (cs.total_circulation::numeric / ep.nopt) * 100), 2) as saturation_pct,
                CASE
                    WHEN (ps.total_stake::numeric / (cs.total_circulation::numeric / ep.nopt) * 100) > 100 THEN 'OVERSATURATED'
                    WHEN (ps.total_stake::numeric / (cs.total_circulation::numeric / ep.nopt) * 100) > 90 THEN 'APPROACHING_SATURATION'
                    ELSE 'HEALTHY'
                END as saturation_level
            FROM pool_stake ps
            CROSS JOIN circulation_supply cs
            CROSS JOIN epoch_params ep
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolIdHex);
        params.put("epoch", epoch);

        String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);

        List<PoolSaturationInfo> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PoolSaturationInfo(
                poolIdHex,
                poolIdBech32,
                rs.getInt("epoch"),
                rs.getBigDecimal("total_stake").toBigInteger(),
                rs.getInt("delegator_count"),
                rs.getBigDecimal("total_circulation").toBigInteger(),
                rs.getInt("nopt"),
                rs.getBigDecimal("optimal_stake_threshold").toBigInteger(),
                rs.getDouble("saturation_pct"),
                rs.getString("saturation_level")
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("Pool not found or has no stake in specified epoch: " + poolId);
        }

        return results.get(0);
    }

    @Tool(name = "pool-luck-analysis",
          description = "Analyze pool luck by comparing actual vs expected blocks produced. " +
                        "Expected blocks = (pool_stake / total_network_stake) × total_epoch_blocks. " +
                        "Luck % = (actual_blocks / expected_blocks) × 100. " +
                        "100% = exactly as expected, >100% = lucky, <100% = unlucky. " +
                        "Returns statistics for each epoch in the range. " +
                        "Pool ID can be bech32 (pool1...) or hex format.")
    public List<PoolLuckStats> getPoolLuckAnalysis(
        @ToolParam(description = "Pool ID (bech32 pool1... or hex)") String poolId,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Analyzing pool luck for: {}, epochs: {}-{}", poolId, startEpoch, endEpoch);

        // Convert bech32 to hex if needed
        String poolIdHex = poolId.startsWith("pool1")
            ? PoolUtil.getPoolHash(poolId)
            : poolId;

        String sql = """
            WITH pool_stake_per_epoch AS (
                SELECT
                    pool_id,
                    active_epoch,
                    SUM(amount) as pool_stake
                FROM epoch_stake
                WHERE pool_id = :poolId
                  AND active_epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY pool_id, active_epoch
            ),
            network_stake_per_epoch AS (
                SELECT
                    active_epoch,
                    SUM(amount) as total_network_stake
                FROM epoch_stake
                WHERE active_epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY active_epoch
            ),
            actual_blocks AS (
                SELECT
                    epoch,
                    COUNT(*) as blocks_produced
                FROM block
                WHERE slot_leader = :poolId
                  AND epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY epoch
            ),
            total_epoch_blocks AS (
                SELECT
                    epoch,
                    COUNT(*) as total_blocks
                FROM block
                WHERE epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY epoch
            )
            SELECT
                ps.active_epoch as epoch,
                ps.pool_id,
                ps.pool_stake,
                ns.total_network_stake,
                ROUND((ps.pool_stake::numeric / ns.total_network_stake * 100), 4) as stake_percentage,
                teb.total_blocks as total_epoch_blocks,
                ROUND((ps.pool_stake::numeric / ns.total_network_stake * teb.total_blocks), 2) as expected_blocks,
                COALESCE(ab.blocks_produced, 0) as actual_blocks,
                CASE
                    WHEN ROUND((ps.pool_stake::numeric / ns.total_network_stake * teb.total_blocks), 2) > 0
                    THEN ROUND((COALESCE(ab.blocks_produced, 0)::numeric / (ps.pool_stake::numeric / ns.total_network_stake * teb.total_blocks) * 100), 2)
                    ELSE 0
                END as luck_percentage
            FROM pool_stake_per_epoch ps
            INNER JOIN network_stake_per_epoch ns ON ps.active_epoch = ns.active_epoch
            LEFT JOIN actual_blocks ab ON ps.active_epoch = ab.epoch
            LEFT JOIN total_epoch_blocks teb ON ps.active_epoch = teb.epoch
            ORDER BY ps.active_epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolIdHex);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PoolLuckStats(
                poolIdHex,
                poolIdBech32,
                rs.getInt("epoch"),
                rs.getBigDecimal("pool_stake").toBigInteger(),
                rs.getBigDecimal("total_network_stake").toBigInteger(),
                rs.getDouble("stake_percentage"),
                rs.getInt("total_epoch_blocks"),
                rs.getDouble("expected_blocks"),
                rs.getInt("actual_blocks"),
                rs.getDouble("luck_percentage")
            )
        );
    }

    @Tool(name = "top-pools-by-stake",
          description = "Get pools ranked by total active stake. " +
                        "Returns top pools ordered by stake amount with saturation info. " +
                        "Useful for finding largest pools and analyzing stake concentration. " +
                        "Optional filters: minStake (in lovelace), excludeOversaturated. " +
                        "Limit controls maximum results (default: 50, max: 200).")
    public List<PoolStakeRanking> getTopPoolsByStake(
        @ToolParam(description = "Minimum stake in lovelace (default: 0)") Long minStake,
        @ToolParam(description = "Exclude oversaturated pools (default: false)") Boolean excludeOversaturated,
        @ToolParam(description = "Active epoch (null for latest)") Integer epoch,
        @ToolParam(description = "Maximum results (default: 50, max: 200)") Integer limit
    ) {
        log.debug("Getting top pools by stake: minStake={}, excludeOversaturated={}, epoch={}, limit={}",
                  minStake, excludeOversaturated, epoch, limit);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;
        long effectiveMinStake = (minStake != null && minStake > 0) ? minStake : 0L;
        boolean shouldExcludeOversaturated = excludeOversaturated != null && excludeOversaturated;

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("minStake", effectiveMinStake);
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
                    HAVING SUM(amount) >= :minStake
                ),
                circulation_supply AS (
                    SELECT circulation as total_circulation
                    FROM adapot
                    WHERE epoch = :epoch
                ),
                epoch_params AS (
                    SELECT (params->>'nopt')::integer as nopt
                    FROM epoch_param
                    WHERE epoch = :epoch
                )
                SELECT
                    pool_id,
                    delegator_count,
                    total_stake,
                    ROUND((total_stake::numeric / (SELECT total_circulation FROM circulation_supply) * 100), 4) as stake_percentage,
                    ROUND((total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100), 2) as saturation_pct,
                    CASE
                        WHEN (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) > 100 THEN 'OVERSATURATED'
                        WHEN (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) > 90 THEN 'APPROACHING_SATURATION'
                        ELSE 'HEALTHY'
                    END as saturation_level,
                    ROW_NUMBER() OVER (ORDER BY total_stake DESC) as rank
                FROM pool_stats
                """ + (shouldExcludeOversaturated ?
                    "WHERE (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) <= 100\n" : "") + """
                ORDER BY rank
                LIMIT :limit
                """;
            params.put("epoch", epoch);
        } else {
            sql = """
                WITH latest_epoch AS (
                    SELECT
                        LEAST(
                            (SELECT MAX(active_epoch) FROM epoch_stake),
                            (SELECT MAX(epoch) FROM adapot)
                        ) as epoch
                ),
                pool_stats AS (
                    SELECT
                        pool_id,
                        COUNT(DISTINCT address) as delegator_count,
                        SUM(amount) as total_stake
                    FROM epoch_stake
                    WHERE active_epoch = (SELECT epoch FROM latest_epoch)
                    GROUP BY pool_id
                    HAVING SUM(amount) >= :minStake
                ),
                circulation_supply AS (
                    SELECT circulation as total_circulation
                    FROM adapot
                    WHERE epoch = (SELECT epoch FROM latest_epoch)
                ),
                epoch_params AS (
                    SELECT (params->>'nopt')::integer as nopt
                    FROM epoch_param
                    WHERE epoch = (SELECT MAX(epoch) FROM epoch_param WHERE epoch <= (SELECT epoch FROM latest_epoch))
                )
                SELECT
                    pool_id,
                    delegator_count,
                    total_stake,
                    ROUND((total_stake::numeric / (SELECT total_circulation FROM circulation_supply) * 100), 4) as stake_percentage,
                    ROUND((total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100), 2) as saturation_pct,
                    CASE
                        WHEN (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) > 100 THEN 'OVERSATURATED'
                        WHEN (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) > 90 THEN 'APPROACHING_SATURATION'
                        ELSE 'HEALTHY'
                    END as saturation_level,
                    ROW_NUMBER() OVER (ORDER BY total_stake DESC) as rank
                FROM pool_stats
                """ + (shouldExcludeOversaturated ?
                    "WHERE (total_stake::numeric / ((SELECT total_circulation FROM circulation_supply) / (SELECT nopt FROM epoch_params)) * 100) <= 100\n" : "") + """
                ORDER BY rank
                LIMIT :limit
                """;
        }

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdHex = rs.getString("pool_id");
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                return new PoolStakeRanking(
                    poolIdHex,
                    poolIdBech32,
                    rs.getInt("delegator_count"),
                    rs.getBigDecimal("total_stake").toBigInteger(),
                    rs.getDouble("stake_percentage"),
                    rs.getDouble("saturation_pct"),
                    rs.getString("saturation_level"),
                    rs.getInt("rank"),
                    epoch
                );
            }
        );
    }

    @Tool(name = "reward-comparison-by-pools",
          description = "Compare reward distribution across multiple pools. " +
                        "Returns side-by-side comparison with normalized metrics (rewards per 10k ADA). " +
                        "Essential for delegators choosing between pools. " +
                        "Pool IDs can be bech32 (pool1...) or hex format. " +
                        "Supports filtering by epoch range.")
    public List<PoolRewardComparison> getRewardComparisonByPools(
        @ToolParam(description = "List of pool IDs (bech32 or hex)") List<String> poolIds,
        @ToolParam(description = "Start epoch (null for all-time)") Integer startEpoch,
        @ToolParam(description = "End epoch (null for all-time)") Integer endEpoch
    ) {
        log.debug("Comparing rewards for {} pools, epochs: {}-{}", poolIds.size(), startEpoch, endEpoch);

        // Convert all pool IDs to hex
        List<String> poolIdsHex = poolIds.stream()
            .map(poolId -> poolId.startsWith("pool1") ? PoolUtil.getPoolHash(poolId) : poolId)
            .toList();

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("poolIds", poolIdsHex);

        if (startEpoch != null && endEpoch != null) {
            sql = """
                WITH pool_rewards AS (
                    SELECT
                        r.pool_id,
                        COALESCE(SUM(r.amount), 0) as total_rewards,
                        COUNT(*) as reward_event_count,
                        COUNT(DISTINCT r.address) as unique_recipients
                    FROM reward r
                    WHERE r.pool_id = ANY(:poolIds)
                      AND r.earned_epoch BETWEEN :startEpoch AND :endEpoch
                    GROUP BY r.pool_id
                ),
                avg_pool_stake AS (
                    SELECT
                        es.pool_id,
                        AVG(es.amount)::numeric as avg_stake_per_delegator,
                        AVG(epoch_total.total_stake)::numeric as avg_pool_stake
                    FROM epoch_stake es
                    INNER JOIN (
                        SELECT pool_id, active_epoch, SUM(amount) as total_stake
                        FROM epoch_stake
                        WHERE active_epoch BETWEEN :startEpoch AND :endEpoch
                        GROUP BY pool_id, active_epoch
                    ) epoch_total ON es.pool_id = epoch_total.pool_id AND es.active_epoch = epoch_total.active_epoch
                    WHERE es.pool_id = ANY(:poolIds)
                      AND es.active_epoch BETWEEN :startEpoch AND :endEpoch
                    GROUP BY es.pool_id
                )
                SELECT
                    pr.pool_id,
                    pr.total_rewards,
                    pr.reward_event_count,
                    pr.unique_recipients,
                    CASE
                        WHEN pr.unique_recipients > 0
                        THEN ROUND(pr.total_rewards::numeric / pr.unique_recipients, 0)
                        ELSE 0
                    END as avg_reward_per_recipient,
                    aps.avg_pool_stake,
                    CASE
                        WHEN aps.avg_pool_stake > 0
                        THEN ROUND((pr.total_rewards::numeric / aps.avg_pool_stake * 10000000000), 0)
                        ELSE 0
                    END as rewards_per_10k_ada
                FROM pool_rewards pr
                LEFT JOIN avg_pool_stake aps ON pr.pool_id = aps.pool_id
                ORDER BY pr.total_rewards DESC
                """;
            params.put("startEpoch", startEpoch);
            params.put("endEpoch", endEpoch);
        } else {
            sql = """
                WITH pool_rewards AS (
                    SELECT
                        r.pool_id,
                        COALESCE(SUM(r.amount), 0) as total_rewards,
                        COUNT(*) as reward_event_count,
                        COUNT(DISTINCT r.address) as unique_recipients
                    FROM reward r
                    WHERE r.pool_id = ANY(:poolIds)
                    GROUP BY r.pool_id
                ),
                avg_pool_stake AS (
                    SELECT
                        es.pool_id,
                        AVG(es.amount)::numeric as avg_stake_per_delegator,
                        AVG(epoch_total.total_stake)::numeric as avg_pool_stake
                    FROM epoch_stake es
                    INNER JOIN (
                        SELECT pool_id, active_epoch, SUM(amount) as total_stake
                        FROM epoch_stake
                        GROUP BY pool_id, active_epoch
                    ) epoch_total ON es.pool_id = epoch_total.pool_id AND es.active_epoch = epoch_total.active_epoch
                    WHERE es.pool_id = ANY(:poolIds)
                    GROUP BY es.pool_id
                )
                SELECT
                    pr.pool_id,
                    pr.total_rewards,
                    pr.reward_event_count,
                    pr.unique_recipients,
                    CASE
                        WHEN pr.unique_recipients > 0
                        THEN ROUND(pr.total_rewards::numeric / pr.unique_recipients, 0)
                        ELSE 0
                    END as avg_reward_per_recipient,
                    aps.avg_pool_stake,
                    CASE
                        WHEN aps.avg_pool_stake > 0
                        THEN ROUND((pr.total_rewards::numeric / aps.avg_pool_stake * 10000000000), 0)
                        ELSE 0
                    END as rewards_per_10k_ada
                FROM pool_rewards pr
                LEFT JOIN avg_pool_stake aps ON pr.pool_id = aps.pool_id
                ORDER BY pr.total_rewards DESC
                """;
        }

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String poolIdHex = rs.getString("pool_id");
                String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);
                return new PoolRewardComparison(
                    poolIdHex,
                    poolIdBech32,
                    rs.getBigDecimal("total_rewards"),
                    rs.getInt("reward_event_count"),
                    rs.getInt("unique_recipients"),
                    rs.getBigDecimal("avg_reward_per_recipient").toBigInteger(),
                    rs.getBigDecimal("avg_pool_stake").toBigInteger(),
                    rs.getBigDecimal("rewards_per_10k_ada").toBigInteger(),
                    startEpoch,
                    endEpoch
                );
            }
        );
    }

    @Tool(name = "delegation-cost-analysis",
          description = "Analyze pool fees and their impact on delegator rewards. " +
                        "Returns detailed breakdown: fixed cost, margin, net rewards after fees. " +
                        "Essential for understanding true ROI after pool operator fees. " +
                        "Calculations based on historical performance over epoch range. " +
                        "Pool ID can be bech32 (pool1...) or hex format.")
    public PoolCostAnalysis getDelegationCostAnalysis(
        @ToolParam(description = "Pool ID (bech32 pool1... or hex)") String poolId,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Analyzing delegation costs for pool: {}, epochs: {}-{}", poolId, startEpoch, endEpoch);

        // Convert bech32 to hex if needed
        String poolIdHex = poolId.startsWith("pool1")
            ? PoolUtil.getPoolHash(poolId)
            : poolId;

        String sql = """
            WITH pool_params AS (
                SELECT
                    pool_id,
                    pledge,
                    cost as fixed_cost,
                    margin::numeric as margin
                FROM pool_registration
                WHERE pool_id = :poolId
                ORDER BY slot DESC
                LIMIT 1
            ),
            pool_stats AS (
                SELECT
                    es.pool_id,
                    AVG(delegator_count)::numeric as avg_delegators,
                    AVG(total_stake)::numeric as avg_pool_stake
                FROM (
                    SELECT
                        pool_id,
                        active_epoch,
                        COUNT(DISTINCT address) as delegator_count,
                        SUM(amount) as total_stake
                    FROM epoch_stake
                    WHERE pool_id = :poolId
                      AND active_epoch BETWEEN :startEpoch AND :endEpoch
                    GROUP BY pool_id, active_epoch
                ) es
                GROUP BY es.pool_id
            ),
            reward_stats AS (
                SELECT
                    pool_id,
                    COALESCE(SUM(amount), 0) as total_rewards,
                    COUNT(DISTINCT earned_epoch) as epoch_count
                FROM reward
                WHERE pool_id = :poolId
                  AND earned_epoch BETWEEN :startEpoch AND :endEpoch
                GROUP BY pool_id
            )
            SELECT
                pp.pool_id,
                pp.fixed_cost,
                pp.margin,
                pp.pledge,
                ps.avg_delegators,
                ps.avg_pool_stake,
                rs.total_rewards,
                rs.epoch_count,
                CASE
                    WHEN ps.avg_delegators > 0 AND rs.epoch_count > 0
                    THEN ROUND(pp.fixed_cost / ps.avg_delegators, 0)
                    ELSE 0
                END as fixed_cost_per_delegator_per_epoch,
                CASE
                    WHEN ps.avg_pool_stake > 0 AND rs.epoch_count > 0
                    THEN ROUND((rs.total_rewards / ps.avg_pool_stake * 10000000000 / rs.epoch_count), 0)
                    ELSE 0
                END as est_rewards_per_10k_ada_per_epoch,
                CASE
                    WHEN ps.avg_pool_stake > 0 AND rs.epoch_count > 0
                    THEN ROUND((rs.total_rewards / ps.avg_pool_stake * 10000000000 / rs.epoch_count * (1 - pp.margin)), 0)
                    ELSE 0
                END as est_rewards_after_margin_per_10k_ada,
                CASE
                    WHEN ps.avg_pool_stake > 0 AND ps.avg_delegators > 0 AND rs.epoch_count > 0
                    THEN ROUND(
                        (rs.total_rewards / ps.avg_pool_stake * 10000000000 / rs.epoch_count * (1 - pp.margin)) -
                        (pp.fixed_cost / ps.avg_delegators),
                        0
                    )
                    ELSE 0
                END as net_rewards_per_10k_ada_per_epoch,
                CASE
                    WHEN ps.avg_pool_stake > 0 AND rs.epoch_count > 0
                    THEN ROUND(
                        ((rs.total_rewards / ps.avg_pool_stake * 10000000000 / rs.epoch_count * (1 - pp.margin)) -
                        (pp.fixed_cost / NULLIF(ps.avg_delegators, 0))) / 10000000000 * 100 * 73,
                        4
                    )
                    ELSE 0
                END as annualized_roa_pct
            FROM pool_params pp
            CROSS JOIN pool_stats ps
            CROSS JOIN reward_stats rs
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolIdHex);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        String poolIdBech32 = PoolUtil.getBech32PoolId(poolIdHex);

        List<PoolCostAnalysis> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PoolCostAnalysis(
                poolIdHex,
                poolIdBech32,
                rs.getBigDecimal("fixed_cost").toBigInteger(),
                rs.getBigDecimal("margin"),
                rs.getBigDecimal("pledge").toBigInteger(),
                rs.getBigDecimal("avg_delegators"),
                rs.getBigDecimal("avg_pool_stake").toBigInteger(),
                rs.getBigDecimal("total_rewards"),
                rs.getInt("epoch_count"),
                rs.getBigDecimal("fixed_cost_per_delegator_per_epoch").toBigInteger(),
                rs.getBigDecimal("est_rewards_per_10k_ada_per_epoch").toBigInteger(),
                rs.getBigDecimal("est_rewards_after_margin_per_10k_ada").toBigInteger(),
                rs.getBigDecimal("net_rewards_per_10k_ada_per_epoch").toBigInteger(),
                rs.getBigDecimal("annualized_roa_pct"),
                startEpoch,
                endEpoch
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("Pool not found or has no data in specified epoch range: " + poolId);
        }

        return results.get(0);
    }
}
