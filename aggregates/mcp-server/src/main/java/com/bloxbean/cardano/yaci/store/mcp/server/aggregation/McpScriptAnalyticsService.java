package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.*;
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
 * MCP service providing script fee analytics for DeFi protocol revenue tracking.
 *
 * Key Features:
 * - Track total fees paid when spending script UTXOs
 * - Identify top DeFi protocols by fee collection
 * - Analyze fee trends over time
 * - Compare multiple protocols
 *
 * Use Cases:
 * - DeFi protocol revenue measurement
 * - Market leader identification
 * - Growth tracking and trend analysis
 * - Competitive analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.transaction.enabled", "store.utxo.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpScriptAnalyticsService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "script-fee-analytics",
          description = "Get fee analytics for script UTXOs. " +
                        "Tracks total fees paid when spending UTXOs from a specific script address. " +
                        "Essential for measuring DeFi protocol revenue and usage. " +
                        "Returns total fees, transaction count, average fee, and unique users.")
    public ScriptFeeAnalytics getScriptFeeAnalytics(
        @ToolParam(description = "Script address") String scriptAddress,
        @ToolParam(description = "Start epoch (optional)", required = false) Integer startEpoch,
        @ToolParam(description = "End epoch (optional)", required = false) Integer endEpoch
    ) {
        log.debug("Getting script fee analytics for address: {}, epochs: {}-{}",
                  scriptAddress, startEpoch, endEpoch);

        String sql = """
            SELECT
                COUNT(DISTINCT t.tx_hash) as tx_count,
                COALESCE(SUM(t.fee), 0) as total_fees,
                COALESCE(AVG(t.fee), 0) as avg_fee,
                COALESCE(MIN(t.fee), 0) as min_fee,
                COALESCE(MAX(t.fee), 0) as max_fee,
                COUNT(DISTINCT ti.spent_tx_hash) as unique_spenders
            FROM address_utxo u
            INNER JOIN tx_input ti ON ti.tx_hash = u.tx_hash
                                   AND ti.output_index = u.output_index
            INNER JOIN transaction t ON t.tx_hash = ti.spent_tx_hash
            WHERE u.owner_addr = :scriptAddress
              AND (:startEpoch IS NULL OR ti.spent_epoch >= :startEpoch)
              AND (:endEpoch IS NULL OR ti.spent_epoch <= :endEpoch)
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("scriptAddress", scriptAddress);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.queryForObject(sql, params,
            (rs, rowNum) -> new ScriptFeeAnalytics(
                scriptAddress,
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getBigDecimal("min_fee"),
                rs.getBigDecimal("max_fee"),
                rs.getInt("unique_spenders")
            )
        );
    }

    @Tool(name = "top-scripts-by-fees",
          description = "Get top N scripts by total fees collected in a time period. " +
                        "Ranks scripts (smart contracts/dApps) by fees paid when their UTXOs are spent. " +
                        "Perfect for identifying most-used DeFi protocols and market leaders. " +
                        "Returns script addresses, fee totals, transaction counts, and unique users.")
    public List<ScriptFeeRanking> getTopScriptsByFees(
        @ToolParam(description = "Number of results (default: 10, max: 100)", required = false) Integer limit,
        @ToolParam(description = "Start epoch (optional)", required = false) Integer startEpoch,
        @ToolParam(description = "End epoch (optional)", required = false) Integer endEpoch,
        @ToolParam(description = "Minimum transactions to filter noise (default: 10)", required = false) Integer minTxCount
    ) {
        int resultLimit = limit != null ? Math.min(limit, 100) : 10;
        int minTx = minTxCount != null ? minTxCount : 10;

        log.debug("Getting top {} scripts by fees, epochs: {}-{}, min tx: {}",
                  resultLimit, startEpoch, endEpoch, minTx);

        String sql = """
            SELECT
                u.owner_addr as script_address,
                COUNT(DISTINCT t.tx_hash) as tx_count,
                COALESCE(SUM(t.fee), 0) as total_fees,
                COALESCE(AVG(t.fee), 0) as avg_fee,
                COUNT(DISTINCT ti.spent_tx_hash) as unique_users,
                MIN(ti.spent_epoch) as first_epoch,
                MAX(ti.spent_epoch) as last_epoch
            FROM address_utxo u
            INNER JOIN tx_input ti ON ti.tx_hash = u.tx_hash
                                   AND ti.output_index = u.output_index
            INNER JOIN transaction t ON t.tx_hash = ti.spent_tx_hash
            WHERE (:startEpoch IS NULL OR ti.spent_epoch >= :startEpoch)
              AND (:endEpoch IS NULL OR ti.spent_epoch <= :endEpoch)
              AND u.owner_addr LIKE 'addr%'
            GROUP BY u.owner_addr
            HAVING COUNT(DISTINCT t.tx_hash) >= :minTxCount
            ORDER BY total_fees DESC
            LIMIT :limit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);
        params.put("minTxCount", minTx);
        params.put("limit", resultLimit);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new ScriptFeeRanking(
                rs.getString("script_address"),
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getInt("unique_users"),
                rs.getInt("first_epoch"),
                rs.getInt("last_epoch")
            )
        );
    }

    @Tool(name = "script-fee-timeline",
          description = "Get script fee collection over time (by epoch). " +
                        "Shows how script usage and revenue evolved. " +
                        "Returns fees per epoch for specified script address. " +
                        "Useful for trend analysis and growth tracking.")
    public List<ScriptFeeTimePoint> getScriptFeeTimeline(
        @ToolParam(description = "Script address") String scriptAddress,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting fee timeline for script: {}, epochs: {}-{}",
                  scriptAddress, startEpoch, endEpoch);

        String sql = """
            SELECT
                ti.spent_epoch as epoch,
                COUNT(DISTINCT t.tx_hash) as tx_count,
                COALESCE(SUM(t.fee), 0) as total_fees,
                COALESCE(AVG(t.fee), 0) as avg_fee,
                COUNT(DISTINCT ti.spent_tx_hash) as unique_users
            FROM address_utxo u
            INNER JOIN tx_input ti ON ti.tx_hash = u.tx_hash
                                   AND ti.output_index = u.output_index
            INNER JOIN transaction t ON t.tx_hash = ti.spent_tx_hash
            WHERE u.owner_addr = :scriptAddress
              AND ti.spent_epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY ti.spent_epoch
            ORDER BY ti.spent_epoch
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("scriptAddress", scriptAddress);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new ScriptFeeTimePoint(
                rs.getInt("epoch"),
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getInt("unique_users")
            )
        );
    }

    @Tool(name = "compare-script-fees",
          description = "Compare fee collection across multiple scripts/protocols. " +
                        "Useful for competitive analysis of DeFi protocols. " +
                        "Returns comparative metrics including revenue per user. " +
                        "Supports up to 10 script addresses for comparison.")
    public List<ScriptFeeComparison> compareScriptFees(
        @ToolParam(description = "Comma-separated script addresses (max 10)") String scriptAddresses,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        String[] addresses = scriptAddresses.split(",");
        if (addresses.length > 10) {
            throw new IllegalArgumentException("Maximum 10 script addresses allowed for comparison");
        }

        log.debug("Comparing {} scripts, epochs: {}-{}", addresses.length, startEpoch, endEpoch);

        String sql = """
            SELECT
                u.owner_addr as script_address,
                COUNT(DISTINCT t.tx_hash) as tx_count,
                COALESCE(SUM(t.fee), 0) as total_fees,
                COALESCE(AVG(t.fee), 0) as avg_fee,
                COUNT(DISTINCT ti.spent_tx_hash) as unique_users,
                CASE
                    WHEN COUNT(DISTINCT ti.spent_tx_hash) > 0
                    THEN COALESCE(SUM(t.fee), 0) / COUNT(DISTINCT ti.spent_tx_hash)
                    ELSE 0
                END as revenue_per_user
            FROM address_utxo u
            INNER JOIN tx_input ti ON ti.tx_hash = u.tx_hash
                                   AND ti.output_index = u.output_index
            INNER JOIN transaction t ON t.tx_hash = ti.spent_tx_hash
            WHERE u.owner_addr = ANY(:scriptAddresses)
              AND ti.spent_epoch BETWEEN :startEpoch AND :endEpoch
            GROUP BY u.owner_addr
            ORDER BY total_fees DESC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("scriptAddresses", addresses);
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new ScriptFeeComparison(
                rs.getString("script_address"),
                rs.getLong("tx_count"),
                rs.getBigDecimal("total_fees"),
                rs.getBigDecimal("avg_fee"),
                rs.getInt("unique_users"),
                rs.getBigDecimal("revenue_per_user")
            )
        );
    }
}
