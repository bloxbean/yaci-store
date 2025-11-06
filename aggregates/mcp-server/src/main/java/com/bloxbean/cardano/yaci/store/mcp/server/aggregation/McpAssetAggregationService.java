package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.AssetMintInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.AssetSearchResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TokenHolderStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

    // DISABLED: Performance issues with token_holder_summary view on mainnet (32+ seconds, timeouts)
    // TODO: Re-enable when alternative strategy is implemented for token holder statistics
    // @Tool(name = "tokens-with-min-holders",
    //       description = "Find tokens that have at least a specified number of unique holders. " +
    //                     "Returns list of tokens (policy_id, asset_name) with holder counts. " +
    //                     "IMPORTANT Token Name Enrichment: Each token includes 'asset_unit' field. " +
    //                     "- If result has <10 tokens: Automatically use 'get-token-registry-metadata' for each to display 'TokenName (TICKER)' " +
    //                     "- If result has 10+ tokens: First show results with hex values. Then ask user if they want human-readable names. " +
    //                     "  If yes, use 'get-token-registry-metadata-batch' with compact=true for efficient bulk fetching. " +
    //                     "This prevents context exhaustion on large result sets. " +
    //                     "Useful for discovering popular tokens and NFT collections. " +
    //                     "Note: Only returns currently unspent UTXOs (active holdings). " +
    //                     "Limit parameter controls how many results to return (default: 20). " +
    //                     "Uses pre-aggregated token_holder_summary view for optimal performance.")
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

    // MOVED: token-holder-stats moved to McpBalanceAggregationService (requires balance tables)
    // This tool now lives in McpBalanceAggregationService.java since it queries address_balance_current table

    // DISABLED: Performance issues with token_holder_summary view on mainnet (32+ seconds, timeouts)
    // TODO: Re-enable when alternative strategy is implemented for token holder statistics
    // @Tool(name = "token-holder-stats-by-policy",
    //       description = "Get holder statistics for all tokens under a specific policy ID. " +
    //                     "Returns list of all assets in the policy with their holder counts. " +
    //                     "IMPORTANT Token Name Enrichment: Each asset includes 'asset_unit' field. " +
    //                     "- If result has <10 tokens: Automatically use 'get-token-registry-metadata' for each to display names " +
    //                     "- If result has 10+ tokens: Show hex values first, then ask user if they want names. " +
    //                     "  If yes, use 'get-token-registry-metadata-batch' with compact=true for efficient bulk fetching. " +
    //                     "For NFT collections, this helps present items as 'Collection Item #1', 'Collection Item #2', etc. " +
    //                     "Useful for analyzing NFT collections or multi-asset policies. " +
    //                     "Only counts currently unspent UTXOs (active holdings). " +
    //                     "Uses pre-aggregated token_holder_summary view for optimal performance.")
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
                        "IMPORTANT Token Name Enrichment: Each token includes 'unit' field. " +
                        "- If result has <10 tokens: Automatically use 'get-token-registry-metadata' to display names " +
                        "- If result has 10+ tokens: Show hex values first, then ask user if they want names. " +
                        "  If yes, use 'get-token-registry-metadata-batch' with compact=true for efficient bulk fetching. " +
                        "This is especially useful for finding burned/delisted tokens that may still have registry entries. " +
                        "Useful for: discovering all tokens in a policy, analyzing mint/burn patterns, finding burned/delisted tokens. " +
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
                        "IMPORTANT Token Name Enrichment: For each newly minted token, the 'unit' field is provided. " +
                        "- If result has <10 tokens: Automatically use 'get-token-registry-metadata' to display verified names: 'Recently minted: TokenName (TICKER)' " +
                        "- If result has 10+ tokens: Show hex values first, then ask user if they want names. " +
                        "  If yes, use 'get-token-registry-metadata-batch' with compact=true for efficient bulk fetching. " +
                        "This helps users discover legitimate new token launches vs spam/scam tokens. " +
                        "Useful for: discovering new token launches, monitoring minting activity, tracking new NFT collections. " +
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

    @Tool(name = "search-assets-by-name",
          description = "Search for assets by their human-readable name (case-insensitive). " +
                        "Returns matching assets with policy ID, unit, and fingerprint. " +
                        "Useful for finding token details when you only know the name (e.g., 'DRIP', 'tDRIP', 'HOSKY'). " +
                        "IMPORTANT: Each result includes 'unit' field - use this with other tools like 'token-holder-stats' or 'get-token-registry-metadata'. " +
                        "Returns top 10 matches ordered by relevance. " +
                        "If 1 match: Returns unique result - proceed with that unit. " +
                        "If multiple matches: Shows list for user selection to avoid ambiguity. " +
                        "If 0 matches: Suggests checking spelling or using partial search. " +
                        "Performance: Uses indexed case-insensitive search (~10-20ms). " +
                        "Partial match option available but slower (~50-200ms on large datasets).")
    public List<AssetSearchResult> searchAssetsByName(
        @ToolParam(description = "Asset name to search for (e.g., 'DRIP', 'tDRIP', 'HOSKY'). Case-insensitive exact match.")
        String assetName,

        @ToolParam(description = "Enable partial matching using pattern search (e.g., finds 'DRIP' in 'MyDRIPToken'). Default: false (exact match only). WARNING: Slower on large datasets.")
        Boolean partialMatch,

        @ToolParam(description = "Optional policy ID to filter results (56-char hex). " +
                                 "Use when multiple tokens share the same name and you want a specific one. " +
                                 "Improves performance and disambiguates results.")
        String policyId
    ) {
        boolean usePartialMatch = partialMatch != null && partialMatch;

        log.debug("[ASSET SEARCH] Searching for asset name: {}, partial: {}, policyId: {}",
                  assetName, usePartialMatch, policyId);

        String sql;
        Map<String, Object> params = new HashMap<>();

        // Handle policy ID - convert empty string to null for proper SQL handling
        String effectivePolicyId = (policyId != null && !policyId.trim().isEmpty()) ? policyId : null;
        params.put("policyId", effectivePolicyId);

        if (usePartialMatch) {
            // Pattern match - uses MATERIALIZED CTE to force correct execution plan
            sql = """
                WITH filtered AS MATERIALIZED (
                    SELECT policy, asset_name, unit, fingerprint
                    FROM assets
                    WHERE asset_name ILIKE :searchPattern
                      AND (NULLIF(:policyId, '') IS NULL OR policy = NULLIF(:policyId, ''))
                )
                SELECT
                    policy,
                    asset_name,
                    unit,
                    MIN(fingerprint) as fingerprint,
                    COUNT(*) as occurrence_count,
                    CASE WHEN LOWER(asset_name) = LOWER(:exactTerm) THEN 0 ELSE 1 END as match_priority
                FROM filtered
                GROUP BY policy, asset_name, unit
                ORDER BY match_priority, asset_name
                LIMIT 10
                """;
            params.put("searchPattern", "%" + assetName + "%");
            params.put("exactTerm", assetName);
        } else {
            // Exact match - uses MATERIALIZED CTE to force idx_assets_asset_name_lower index usage
            sql = """
                WITH filtered AS MATERIALIZED (
                    SELECT policy, asset_name, unit, fingerprint
                    FROM assets
                    WHERE LOWER(asset_name) = LOWER(:searchTerm)
                      AND (NULLIF(:policyId, '') IS NULL OR policy = NULLIF(:policyId, ''))
                )
                SELECT
                    policy,
                    asset_name,
                    unit,
                    MIN(fingerprint) as fingerprint,
                    COUNT(*) as occurrence_count
                FROM filtered
                GROUP BY policy, asset_name, unit
                ORDER BY policy
                LIMIT 10
                """;
            params.put("searchTerm", assetName);
        }

        List<AssetSearchResult> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new AssetSearchResult(
                rs.getString("policy"),
                rs.getString("asset_name"),
                rs.getString("unit"),
                rs.getString("fingerprint"),
                rs.getInt("occurrence_count")
            )
        );

        log.debug("[ASSET SEARCH] Found {} matches for '{}'", results.size(), assetName);

        return results;
    }
}
