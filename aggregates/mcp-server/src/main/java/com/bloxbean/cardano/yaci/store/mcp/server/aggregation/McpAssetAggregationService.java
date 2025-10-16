package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.AssetMintInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TokenHolderStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * MCP service providing optimized asset aggregation queries.
 * Focuses on token holder analysis and asset distribution statistics.
 *
 * Key Features:
 * - Token holder counting with policy_id extraction from JSONB
 * - Asset distribution analysis
 * - Top token discovery by holder count
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.utxo.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpAssetAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "tokens-with-min-holders",
          description = "Find tokens that have at least a specified number of unique holders. " +
                        "Returns list of tokens (policy_id, asset_name) with holder counts. " +
                        "Useful for discovering popular tokens and NFT collections. " +
                        "Note: Only returns currently unspent UTXOs (active holdings). " +
                        "Limit parameter controls how many results to return (default: 20). " +
                        "Uses pre-aggregated token_holder_summary view for optimal performance.")
    public List<TokenHolderStats> getTokensWithMinHolders(
        @ToolParam(description = "Minimum number of unique holders required") int minHolders,
        @ToolParam(description = "Maximum number of results to return (default: 20)") Integer limit
    ) {
        log.debug("Finding tokens with at least {} holders, limit: {}", minHolders, limit);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;

        String sql = """
            SELECT
                policy_id,
                asset_name,
                asset_unit,
                holder_count,
                total_supply,
                utxo_count
            FROM token_holder_summary
            WHERE holder_count >= :minHolders
            ORDER BY holder_count DESC, total_supply DESC
            LIMIT :limit
            """;

        return jdbcTemplate.query(sql,
            Map.of("minHolders", minHolders, "limit", effectiveLimit),
            (rs, rowNum) -> new TokenHolderStats(
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getString("asset_unit"),
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_supply"),
                rs.getInt("utxo_count")
            )
        );
    }

    @Tool(name = "token-holder-stats",
          description = "Get detailed holder statistics for a specific token by asset unit. " +
                        "Returns holder count, total supply, and UTXO distribution. " +
                        "Asset unit format: policyId + assetName (hex). " +
                        "Only counts currently unspent UTXOs (active holdings). " +
                        "Uses pre-aggregated token_holder_summary view for optimal performance.")
    public TokenHolderStats getTokenHolderStats(
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit
    ) {
        log.debug("Getting holder stats for asset: {}", assetUnit);

        String sql = """
            SELECT
                policy_id,
                asset_name,
                asset_unit,
                holder_count,
                total_supply,
                utxo_count
            FROM token_holder_summary
            WHERE asset_unit = :assetUnit
            """;

        List<TokenHolderStats> results = jdbcTemplate.query(sql,
            Map.of("assetUnit", assetUnit),
            (rs, rowNum) -> new TokenHolderStats(
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getString("asset_unit"),
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_supply"),
                rs.getInt("utxo_count")
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("No holder data found for asset: " + assetUnit);
        }

        return results.get(0);
    }

    @Tool(name = "token-holder-stats-by-policy",
          description = "Get holder statistics for all tokens under a specific policy ID. " +
                        "Returns list of all assets in the policy with their holder counts. " +
                        "Useful for analyzing NFT collections or multi-asset policies. " +
                        "Only counts currently unspent UTXOs (active holdings). " +
                        "Uses pre-aggregated token_holder_summary view for optimal performance.")
    public List<TokenHolderStats> getTokenHolderStatsByPolicy(
        @ToolParam(description = "Policy ID (hex)") String policyId
    ) {
        log.debug("Getting holder stats for policy: {}", policyId);

        String sql = """
            SELECT
                policy_id,
                asset_name,
                asset_unit,
                holder_count,
                total_supply,
                utxo_count
            FROM token_holder_summary
            WHERE policy_id = :policyId
            ORDER BY holder_count DESC, total_supply DESC
            """;

        return jdbcTemplate.query(sql,
            Map.of("policyId", policyId),
            (rs, rowNum) -> new TokenHolderStats(
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getString("asset_unit"),
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_supply"),
                rs.getInt("utxo_count")
            )
        );
    }

    @Tool(name = "find-tokens-by-policy-history",
          description = "Find all tokens minted under a specific policy ID from the assets table. " +
                        "Returns complete mint/burn history including tokens with zero current supply. " +
                        "Useful for: discovering all tokens in a policy, analyzing mint/burn patterns, " +
                        "finding burned/delisted tokens. " +
                        "Aggregates by policy and asset_name, showing net quantity (mints - burns).")
    public List<AssetMintInfo> findTokensByPolicyHistory(
        @ToolParam(description = "Policy ID (hex)") String policyId,
        @ToolParam(description = "Maximum number of results (default: 50, max: 200)") Integer limit
    ) {
        log.debug("Finding token mint/burn history for policy: {}", policyId);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;

        String sql = """
            SELECT
                policy,
                asset_name,
                unit,
                fingerprint,
                SUM(quantity) as net_quantity,
                COUNT(*) as mint_burn_count,
                MIN(slot) as first_mint_slot,
                MAX(slot) as last_activity_slot,
                MIN(block_time) as first_mint_time,
                MAX(block_time) as last_activity_time
            FROM assets
            WHERE policy = :policyId
            GROUP BY policy, asset_name, unit, fingerprint
            ORDER BY first_mint_slot ASC
            LIMIT :limit
            """;

        return jdbcTemplate.query(sql,
            Map.of("policyId", policyId, "limit", effectiveLimit),
            (rs, rowNum) -> new AssetMintInfo(
                rs.getString("policy"),
                rs.getString("asset_name"),
                rs.getString("unit"),
                rs.getString("fingerprint"),
                rs.getBigDecimal("net_quantity"),
                rs.getInt("mint_burn_count"),
                rs.getLong("first_mint_slot"),
                rs.getLong("last_activity_slot"),
                rs.getLong("first_mint_time"),
                rs.getLong("last_activity_time")
            )
        );
    }

    @Tool(name = "find-recent-token-mints",
          description = "Find recently minted tokens across all policies. " +
                        "Returns tokens that had mint transactions within the specified slot range. " +
                        "Useful for: discovering new token launches, monitoring minting activity, " +
                        "tracking new NFT collections. " +
                        "Results grouped by policy and asset, showing aggregated mint data.")
    public List<AssetMintInfo> findRecentTokenMints(
        @ToolParam(description = "Number of recent slots to search (e.g., 7200 slots = ~2 hours)") Long slotRange,
        @ToolParam(description = "Maximum results (default: 50, max: 200)") Integer limit
    ) {
        log.debug("Finding tokens minted in last {} slots, limit: {}", slotRange, limit);

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;

        String sql = """
            SELECT
                policy,
                asset_name,
                unit,
                fingerprint,
                SUM(quantity) as net_quantity,
                COUNT(*) as mint_burn_count,
                MIN(slot) as first_mint_slot,
                MAX(slot) as last_activity_slot,
                MIN(block_time) as first_mint_time,
                MAX(block_time) as last_activity_time
            FROM assets
            WHERE slot >= (SELECT COALESCE(MAX(slot), 0) FROM assets) - :slotRange
              AND quantity > 0
            GROUP BY policy, asset_name, unit, fingerprint
            ORDER BY first_mint_slot DESC
            LIMIT :limit
            """;

        return jdbcTemplate.query(sql,
            Map.of("slotRange", slotRange, "limit", effectiveLimit),
            (rs, rowNum) -> new AssetMintInfo(
                rs.getString("policy"),
                rs.getString("asset_name"),
                rs.getString("unit"),
                rs.getString("fingerprint"),
                rs.getBigDecimal("net_quantity"),
                rs.getInt("mint_burn_count"),
                rs.getLong("first_mint_slot"),
                rs.getLong("last_activity_slot"),
                rs.getLong("first_mint_time"),
                rs.getLong("last_activity_time")
            )
        );
    }
}