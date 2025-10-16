package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.EpochBlockStats;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PoolProductionStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service providing block and pool aggregation analytics.
 *
 * Key Features:
 * - Stake pool block production statistics
 * - Transaction processing metrics
 * - Fee collection by pool
 * - Performance analysis across epochs
 *
 * Use Cases:
 * - Pool performance monitoring
 * - Operator metrics tracking
 * - Delegation decision support
 * - Network decentralization analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.blocks.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpBlockAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "pool-block-production-stats",
          description = "Get block production statistics for a pool across epochs. " +
                        "Returns blocks produced, transactions processed, fees collected per epoch. " +
                        "Essential for pool performance analysis and operator monitoring. " +
                        "Supports both bech32 pool IDs and hex formats.")
    public List<PoolProductionStats> getPoolStats(
        @ToolParam(description = "Pool ID (bech32 or hex)") String poolId,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting pool production stats for pool: {}, epochs: {}-{}",
                  poolId, startEpoch, endEpoch);

        String sql = """
            SELECT
                epoch,
                COUNT(*) as blocks_produced,
                COALESCE(SUM(no_of_txs), 0) as total_transactions,
                COALESCE(SUM(total_fees), 0) as total_fees,
                COALESCE(AVG(no_of_txs), 0) as avg_txs_per_block
            FROM block
            WHERE slot_leader = :poolId
              AND epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY epoch
            ORDER BY epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("poolId", poolId);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PoolProductionStats(
                rs.getInt("epoch"),
                rs.getInt("blocks_produced"),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("total_fees"),
                rs.getDouble("avg_txs_per_block")
            )
        );
    }

    @Tool(name = "epoch-block-statistics",
          description = "Get network-wide block production statistics per epoch. " +
                        "Returns total blocks, transactions, fees, and pool diversity metrics. " +
                        "Essential for network health monitoring and decentralization analysis. " +
                        "Shows avg block size, tx per block, and unique pool participation.")
    public List<EpochBlockStats> getEpochBlockStatistics(
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting epoch block statistics for epochs: {}-{}", startEpoch, endEpoch);

        String sql = """
            SELECT
                epoch,
                COUNT(*) as block_count,
                COALESCE(SUM(no_of_txs), 0) as total_transactions,
                COALESCE(SUM(total_fees), 0) as total_fees,
                COALESCE(AVG(no_of_txs), 0) as avg_txs_per_block,
                COALESCE(AVG(body_size), 0) as avg_block_size,
                COUNT(DISTINCT slot_leader) as unique_pool_count
            FROM block
            WHERE epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY epoch
            ORDER BY epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new EpochBlockStats(
                rs.getInt("epoch"),
                rs.getInt("block_count"),
                rs.getLong("total_transactions"),
                rs.getBigDecimal("total_fees"),
                rs.getDouble("avg_txs_per_block"),
                rs.getDouble("avg_block_size"),
                rs.getInt("unique_pool_count")
            )
        );
    }
}
