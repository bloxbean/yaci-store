package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.BlockTransactionStats;
import com.bloxbean.cardano.yaci.store.mcp.server.model.EpochTransactionStats;
import com.bloxbean.cardano.yaci.store.mcp.server.model.FeeDistributionStats;
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
 * MCP service providing transaction aggregation analytics.
 *
 * Key Features:
 * - Epoch-level transaction statistics
 * - Fee analysis and trends
 * - Valid vs invalid transaction tracking
 * - Network activity metrics
 *
 * Use Cases:
 * - Network usage analysis
 * - Fee trend monitoring
 * - Epoch comparison
 * - Transaction volume tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.transaction.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpTransactionAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "epoch-transaction-statistics",
          description = "Get transaction statistics for epoch range. " +
                        "Returns tx count, total fees, average fee, total output per epoch. " +
                        "Useful for network usage analysis and fee trends. " +
                        "Includes valid/invalid transaction breakdown.")
    public List<EpochTransactionStats> getEpochStatistics(
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting transaction statistics for epochs: {}-{}", startEpoch, endEpoch);

        String sql = """
            SELECT
                epoch,
                COUNT(*) as tx_count,
                COALESCE(SUM(fee), 0) as total_fees,
                COALESCE(AVG(fee), 0) as avg_fee,
                COUNT(DISTINCT block) as block_count,
                SUM(CASE WHEN invalid = false THEN 1 ELSE 0 END) as valid_tx_count,
                SUM(CASE WHEN invalid = true THEN 1 ELSE 0 END) as invalid_tx_count
            FROM transaction
            WHERE epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY epoch
            ORDER BY epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new EpochTransactionStats(
                rs.getInt("epoch"),
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getInt("block_count"),
                rs.getLong("valid_tx_count"),
                rs.getLong("invalid_tx_count")
            )
        );
    }

    @Tool(name = "block-transaction-statistics",
          description = "Get transaction statistics for specific blocks. " +
                        "Returns detailed metrics per block including tx count, fees, and validity. " +
                        "Useful for block-level analysis and block producer performance. " +
                        "Supports epoch range filtering.")
    public List<BlockTransactionStats> getBlockStatistics(
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch,
        @ToolParam(description = "Minimum transactions per block (optional, default: 0)") Integer minTxCount
    ) {
        log.debug("Getting block transaction statistics for epochs: {}-{}", startEpoch, endEpoch);

        int minTx = minTxCount != null ? minTxCount : 0;

        String sql = """
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
            WHERE b.epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY b.number, b.hash, b.epoch, b.slot
            HAVING COUNT(t.tx_hash) >= :minTxCount
            ORDER BY b.number DESC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);
        params.put("minTxCount", minTx);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new BlockTransactionStats(
                rs.getLong("block_number"),
                rs.getString("block_hash"),
                rs.getInt("epoch"),
                rs.getLong("slot"),
                rs.getInt("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getInt("valid_tx_count"),
                rs.getInt("invalid_tx_count")
            )
        );
    }

    @Tool(name = "fee-distribution-analysis",
          description = "Get fee distribution statistics with percentiles for epoch range. " +
                        "Returns min, max, avg, median, and percentile breakdowns (p25, p75, p90, p95, p99). " +
                        "Essential for understanding fee market dynamics and transaction cost trends. " +
                        "Helps identify outliers and typical fee patterns.")
    public List<FeeDistributionStats> getFeeDistribution(
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting fee distribution analysis for epochs: {}-{}", startEpoch, endEpoch);

        String sql = """
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
            WHERE epoch BETWEEN :startEpoch AND :endEpoch
              AND invalid = false
            GROUP BY epoch
            ORDER BY epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new FeeDistributionStats(
                rs.getInt("epoch"),
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("min_fee"),
                rs.getBigDecimal("max_fee"),
                rs.getBigDecimal("avg_fee"),
                rs.getBigDecimal("median_fee"),
                rs.getBigDecimal("p25_fee"),
                rs.getBigDecimal("p75_fee"),
                rs.getBigDecimal("p90_fee"),
                rs.getBigDecimal("p95_fee"),
                rs.getBigDecimal("p99_fee")
            )
        );
    }
}
