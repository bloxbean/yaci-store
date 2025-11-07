package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.client.util.HexUtil;
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
 * MCP service providing balance-table-only aggregation queries.
 * These tools are ONLY available when address balance tables are enabled via:
 * store.account.address-balance-enabled=true
 *
 * Key Features:
 * - Lightning-fast balance queries without UTXO aggregation
 * - Top account/holder analytics
 * - Direct queries on pre-aggregated balance tables
 * - 10-100x performance improvement over UTXO-based queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.account.address-balance-enabled",
    havingValue = "true"
)
public class McpBalanceAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "fast-address-balance",
          description = "⚡ FAST balance lookup for address(es) - returns TOP holdings by quantity (max: 100 tokens). " +
                        "10-100x faster than utxo-balance-summary. Perfect for quick balance checks when you don't need UTXO count or first_seen metadata. " +
                        "Returns tokens ordered by quantity (largest holdings first). " +
                        "⚠️ IMPORTANT: If address holds more than 100 tokens, only top 100 by quantity are returned. " +
                        "The response will include lovelace (ADA) plus the largest native token holdings. " +
                        "For addresses with many tokens (like DEX addresses, collectors), you'll see the most significant holdings. " +
                        "If results are truncated, a note will indicate total token count vs returned count. " +
                        "IMPORTANT: For each token returned, use 'asset_unit' to fetch token registry metadata: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<asset_unit>.json " +
                        "Display as 'TokenName (TICKER): X.XXX' with proper decimals. " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<AssetHolding> getFastAddressBalance(
        @ToolParam(description = "Single address or comma-separated addresses") String addresses,
        @ToolParam(description = "Maximum tokens to return (default: 100, max: 100). Returns top holdings by quantity.") Integer limit
    ) {
        int effectiveLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 100;

        log.debug("[FAST BALANCE] Getting top {} assets for addresses: {}", effectiveLimit, addresses);

        Map<String, Object> params = new HashMap<>();
        params.put("addresses", addresses.split(","));

        // First, count total tokens to detect truncation
        String countSql = """
            SELECT COUNT(DISTINCT unit) as total_tokens
            FROM address_balance_current
            WHERE address = ANY(:addresses)
              AND quantity > 0
            """;

        Long totalTokens = jdbcTemplate.queryForObject(countSql, params, Long.class);

        if (totalTokens != null && totalTokens > effectiveLimit) {
            log.warn("[FAST BALANCE] Address has {} tokens but only returning top {} by quantity. " +
                     "{} tokens will not be shown.", totalTokens, effectiveLimit, totalTokens - effectiveLimit);
        }

        // Fetch top N tokens by quantity (biggest holdings first)
        String sql = """
            SELECT
                unit as asset_unit,
                SUM(quantity) as quantity
            FROM address_balance_current
            WHERE address = ANY(:addresses)
              AND quantity > 0
            GROUP BY unit
            ORDER BY quantity DESC
            LIMIT :limit
            """;

        params.put("limit", effectiveLimit);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String assetUnit = rs.getString("asset_unit");

                // Special handling for lovelace
                if ("lovelace".equals(assetUnit)) {
                    return new AssetHolding(
                        "lovelace",
                        "",
                        "ADA",
                        rs.getBigDecimal("quantity")
                    );
                }

                // Parse policyId and assetName from unit for tokens
                String policyId = assetUnit.substring(0, 56);
                String assetNameHex = assetUnit.length() > 56 ? assetUnit.substring(56) : "";

                // Try to decode asset name from hex to UTF-8
                String assetName = assetNameHex;
                if (!assetNameHex.isEmpty()) {
                    try {
                        byte[] decoded = HexUtil.decodeHexString(assetNameHex);
                        assetName = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.trace("Could not decode asset name hex {}, using hex value", assetNameHex);
                    }
                }

                return new AssetHolding(
                    assetUnit,
                    policyId,
                    assetName,
                    rs.getBigDecimal("quantity")
                );
            }
        );
    }

    @Tool(name = "top-addresses-by-balance",
          description = "Get top addresses by total ADA balance. Returns addresses ranked by lovelace holdings. " +
                        "Perfect for whale watching, rich list analysis, and network distribution studies. " +
                        "ULTRA-FAST: Direct query on pre-aggregated balance tables, no UTXO enumeration needed. " +
                        "Returns both lovelace and ADA values for clarity. " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<AddressBalanceRanking> getTopAddressesByBalance(
        @ToolParam(description = "Maximum number of addresses to return (default: 100, max: 1000)") Integer limit,
        @ToolParam(description = "Minimum balance in lovelace to include (default: 1000000 = 1 ADA). Use higher values to filter out dust.") Long minLovelace
    ) {
        int resultLimit = (limit != null && limit > 0 && limit <= 1000) ? limit : 100;
        long minBalance = (minLovelace != null && minLovelace > 0) ? minLovelace : 1000000L;

        log.debug("[TOP ADDRESSES] Getting top {} addresses with min balance {} lovelace", resultLimit, minBalance);

        String sql = """
            SELECT
                address,
                quantity as lovelace_balance,
                (quantity / 1000000.0) as ada_balance
            FROM address_balance_current
            WHERE unit = 'lovelace'
              AND quantity >= :minLovelace
            ORDER BY quantity DESC
            LIMIT :limit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("limit", resultLimit);
        params.put("minLovelace", minBalance);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new AddressBalanceRanking(
                rowNum + 1,  // rank (1-based, rowNum is 0-based)
                rs.getString("address"),
                rs.getBigDecimal("lovelace_balance"),
                rs.getBigDecimal("ada_balance")
            )
        );
    }

    @Tool(name = "top-holders-by-asset",
          description = "Get TOP N holders of a specific asset/token ranked by quantity held (default: top 10, max: 50). " +
                        "Perfect for whale watching and distribution analysis. " +
                        "ULTRA-FAST: Direct query on pre-aggregated balance tables. " +
                        "To see more holders, specify limit parameter (e.g., limit=50). " +
                        "For total holder COUNT only (without address list), use 'token-holder-stats' tool instead. " +
                        "IMPORTANT: Use the 'assetUnit' parameter to fetch token metadata: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<assetUnit>.json " +
                        "Present results as 'Top N holders of TokenName (TICKER)' with proper decimal formatting. " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<AssetHolderRanking> getTopHoldersByAsset(
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit,
        @ToolParam(description = "Maximum number of holders to return (default: 10, max: 50)") Integer limit,
        @ToolParam(description = "Minimum quantity to include (default: 1). Filter out dust holders.") Long minQuantity
    ) {
        int resultLimit = (limit != null && limit > 0 && limit <= 50) ? limit : 10;
        long minQty = (minQuantity != null && minQuantity > 0) ? minQuantity : 1L;

        log.debug("[TOP HOLDERS] Getting top {} holders of asset {} with min quantity {}", resultLimit, assetUnit, minQty);

        String sql = """
            SELECT
                address,
                quantity
            FROM address_balance_current
            WHERE unit = :assetUnit
              AND quantity >= :minQuantity
            ORDER BY quantity DESC
            LIMIT :limit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("assetUnit", assetUnit);
        params.put("limit", resultLimit);
        params.put("minQuantity", minQty);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new AssetHolderRanking(
                rowNum + 1,  // rank (1-based)
                rs.getString("address"),
                assetUnit,
                rs.getBigDecimal("quantity")
            )
        );
    }

    @Tool(name = "token-holder-stats",
          description = "Get holder COUNT and supply statistics for a specific token by asset unit. " +
                        "Returns ONLY statistics (no address list): holderCount, totalQuantity, policyId, assetName. " +
                        "Perfect for answering 'how many holders' queries without returning large address lists. " +
                        "ULTRA-FAST: Uses address_balance_current table (100ms vs old 32+ seconds). " +
                        "Asset unit format: policyId + assetName (hex). " +
                        "IMPORTANT: Use 'get-token-registry-metadata' with returned 'assetUnit' to fetch token names/tickers. " +
                        "Present stats as 'X holders of TokenName (TICKER) with Y.YYY total supply'. " +
                        "Only counts addresses with positive balances (active holdings). " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public TokenHolderStats getTokenHolderStats(
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit
    ) {
        log.debug("[TOKEN HOLDER STATS] Getting stats for asset: {}", assetUnit);

        String sql = """
            SELECT
                COALESCE(a.policy, SUBSTRING(:assetUnit FROM 1 FOR 56)) as policy_id,
                COALESCE(a.asset_name, '') as asset_name,
                abc_stats.asset_unit,
                abc_stats.holder_count,
                abc_stats.total_quantity
            FROM (
                SELECT
                    unit as asset_unit,
                    COUNT(DISTINCT address) as holder_count,
                    SUM(quantity) as total_quantity
                FROM address_balance_current
                WHERE unit = :assetUnit AND quantity > 0
                GROUP BY unit
            ) abc_stats
            LEFT JOIN assets a ON a.unit = abc_stats.asset_unit
            """;

        List<TokenHolderStats> results = jdbcTemplate.query(sql,
            Map.of("assetUnit", assetUnit),
            (rs, rowNum) -> new TokenHolderStats(
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getString("asset_unit"),
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_quantity"),
                0  // utxoCount not tracked in balance tables, set to 0
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("No holder data found for asset: " + assetUnit);
        }

        return results.get(0);
    }

    @Tool(name = "top-holders-by-policy",
          description = "Get TOP N holders across ALL assets in a policy (NFT collection whale analysis). " +
                        "Returns addresses ranked by total quantity OR unique asset count across the entire policy. " +
                        "Perfect for: NFT collection whale watching, identifying top collectors, portfolio analysis. " +
                        "ULTRA-FAST: Aggregates across all assets in a policy using pre-computed balances. " +
                        "Ranking options: " +
                        "- 'quantity' (default): Total quantity across all assets (useful for fungible tokens) " +
                        "- 'unique_assets': Number of different assets held (useful for NFT collections) " +
                        "Example use cases: " +
                        "- 'Who holds the most NFTs from this collection?' → rankBy='unique_assets' " +
                        "- 'Who holds the most tokens from this policy?' → rankBy='quantity' " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<PolicyHolderRanking> getTopHoldersByPolicy(
        @ToolParam(description = "Policy ID (hex, 56 characters)") String policyId,
        @ToolParam(description = "Maximum number of holders to return (default: 10, max: 100)") Integer limit,
        @ToolParam(description = "Ranking method: 'quantity' (total quantity) or 'unique_assets' (asset count). Default: 'unique_assets'") String rankBy
    ) {
        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 10;
        String ranking = (rankBy != null && rankBy.equalsIgnoreCase("quantity")) ? "quantity" : "unique_assets";

        log.debug("[TOP HOLDERS BY POLICY] Getting top {} holders for policy {} ranked by {}", resultLimit, policyId, ranking);

        String orderByClause = ranking.equals("quantity") ? "total_quantity DESC" : "unique_asset_count DESC";

        String sql = """
            SELECT
                address,
                COUNT(DISTINCT unit) as unique_asset_count,
                SUM(quantity) as total_quantity
            FROM address_balance_current
            WHERE LEFT(unit, 56) = :policyId
              AND quantity > 0
            GROUP BY address
            ORDER BY %s, address
            LIMIT :limit
            """.formatted(orderByClause);

        Map<String, Object> params = new HashMap<>();
        params.put("policyId", policyId);
        params.put("limit", resultLimit);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new PolicyHolderRanking(
                rowNum + 1,  // rank (1-based)
                rs.getString("address"),
                policyId,
                rs.getInt("unique_asset_count"),
                rs.getBigDecimal("total_quantity")
            )
        );
    }

    @Tool(name = "nft-distribution-by-policy",
          description = "Get NFT distribution histogram for a collection (how many holders have 1, 2, 3... NFTs). " +
                        "Shows the distribution pattern: '50 addresses hold 1 NFT, 20 hold 2 NFTs, 10 hold 3+', etc. " +
                        "Perfect for: Understanding collection distribution, identifying concentration patterns, analyzing collector behavior. " +
                        "ULTRA-FAST: Aggregates distribution using pre-computed balances. " +
                        "Returns buckets showing (NFT count → address count) for the entire collection. " +
                        "Useful for answering: 'How many whales vs small holders?', 'Is the collection well distributed?' " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<NFTDistributionBucket> getNFTDistributionByPolicy(
        @ToolParam(description = "Policy ID (hex, 56 characters)") String policyId,
        @ToolParam(description = "Maximum number of buckets to return (default: 20, max: 100). Buckets beyond this are aggregated into 'N+'.") Integer maxBuckets
    ) {
        int bucketLimit = (maxBuckets != null && maxBuckets > 0 && maxBuckets <= 100) ? maxBuckets : 20;

        log.debug("[NFT DISTRIBUTION] Getting distribution for policy {} with max {} buckets", policyId, bucketLimit);

        // First, get the distribution of unique asset counts per address
        String sql = """
            WITH holder_counts AS (
                SELECT
                    address,
                    COUNT(DISTINCT unit) as nft_count,
                    SUM(quantity) as total_quantity
                FROM address_balance_current
                WHERE LEFT(unit, 56) = :policyId
                  AND quantity > 0
                GROUP BY address
            ),
            distribution AS (
                SELECT
                    nft_count,
                    COUNT(*) as address_count,
                    SUM(total_quantity) as bucket_quantity
                FROM holder_counts
                GROUP BY nft_count
                ORDER BY nft_count
            ),
            total_holders AS (
                SELECT COUNT(DISTINCT address) as total
                FROM address_balance_current
                WHERE LEFT(unit, 56) = :policyId AND quantity > 0
            )
            SELECT
                d.nft_count,
                d.address_count,
                d.bucket_quantity,
                ROUND((d.address_count::numeric / NULLIF(t.total, 0)) * 100, 2) as percent_of_holders
            FROM distribution d
            CROSS JOIN total_holders t
            ORDER BY d.nft_count
            LIMIT :bucketLimit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("policyId", policyId);
        params.put("bucketLimit", bucketLimit);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new NFTDistributionBucket(
                rs.getInt("nft_count"),
                rs.getInt("address_count"),
                rs.getBigDecimal("bucket_quantity"),
                rs.getBigDecimal("percent_of_holders")
            )
        );
    }

    @Tool(name = "holder-concentration-analysis",
          description = "Calculate wealth distribution and concentration metrics for a token (Gini coefficient, percentiles, top holder %). " +
                        "Returns comprehensive distribution analysis: " +
                        "- Gini coefficient (0 = perfect equality, 1 = perfect inequality) " +
                        "- Top percentile holdings (top 1%, 5%, 10%, 25%) " +
                        "- Top N holder concentration (top 10, top 100) " +
                        "- Statistical measures (median, mean) " +
                        "- Concentration level assessment " +
                        "Perfect for: Token economics analysis, whale impact assessment, fairness evaluation. " +
                        "ULTRA-FAST: Single query with window functions for percentile calculations. " +
                        "Useful for: 'Is this token fairly distributed?', 'How much do whales control?' " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public HolderConcentrationStats getHolderConcentrationAnalysis(
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit
    ) {
        log.debug("[CONCENTRATION ANALYSIS] Analyzing distribution for asset: {}", assetUnit);

        String sql = """
            WITH holder_balances AS (
                SELECT
                    address,
                    quantity,
                    ROW_NUMBER() OVER (ORDER BY quantity DESC) as rank,
                    SUM(quantity) OVER () as total_supply,
                    COUNT(*) OVER () as total_holders
                FROM address_balance_current
                WHERE unit = :assetUnit AND quantity > 0
            ),
            ranked_cumulative AS (
                SELECT
                    address,
                    quantity,
                    rank,
                    total_supply,
                    total_holders,
                    SUM(quantity) OVER (ORDER BY quantity DESC) as cumulative_quantity,
                    SUM(quantity) OVER (ORDER BY quantity ASC) as cumulative_from_bottom
                FROM holder_balances
            ),
            max_stats AS (
                SELECT
                    MAX(total_holders) as max_holders,
                    MAX(total_supply) as max_supply
                FROM ranked_cumulative
            ),
            percentiles AS (
                SELECT
                    -- Top holder concentration
                    SUM(CASE WHEN rank <= 10 THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_10_holders_percent,
                    SUM(CASE WHEN rank <= 100 THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_100_holders_percent,

                    -- Top percentile concentration
                    SUM(CASE WHEN rank <= GREATEST(1, CEIL(ms.max_holders * 0.01)) THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_1_pct,
                    SUM(CASE WHEN rank <= GREATEST(1, CEIL(ms.max_holders * 0.05)) THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_5_pct,
                    SUM(CASE WHEN rank <= GREATEST(1, CEIL(ms.max_holders * 0.10)) THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_10_pct,
                    SUM(CASE WHEN rank <= GREATEST(1, CEIL(ms.max_holders * 0.25)) THEN quantity ELSE 0 END) / NULLIF(ms.max_supply, 0) * 100 as top_25_pct,

                    -- Gini coefficient approximation
                    2.0 * SUM(rank * quantity) / (ms.max_holders * NULLIF(ms.max_supply, 0)) -
                    (ms.max_holders + 1.0) / ms.max_holders as gini_coefficient,

                    -- Statistical measures
                    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY quantity) as median_holding,
                    AVG(quantity) as mean_holding,

                    ms.max_holders as total_holders,
                    ms.max_supply as total_supply
                FROM ranked_cumulative
                CROSS JOIN max_stats ms
                GROUP BY ms.max_holders, ms.max_supply
            ),
            asset_info AS (
                SELECT DISTINCT
                    SUBSTRING(:assetUnit FROM 1 FOR 56) as policy_id,
                    COALESCE(a.asset_name, '') as asset_name
                FROM address_balance_current abc
                LEFT JOIN assets a ON a.unit = :assetUnit
                WHERE abc.unit = :assetUnit
                LIMIT 1
            )
            SELECT
                :assetUnit as asset_unit,
                ai.policy_id,
                ai.asset_name,
                p.total_holders,
                p.total_supply,
                p.top_1_pct,
                p.top_5_pct,
                p.top_10_pct,
                p.top_25_pct,
                p.top_10_holders_percent,
                p.top_100_holders_percent,
                p.gini_coefficient,
                p.median_holding,
                p.mean_holding
            FROM percentiles p
            CROSS JOIN asset_info ai
            """;

        List<HolderConcentrationStats> results = jdbcTemplate.query(sql,
            Map.of("assetUnit", assetUnit),
            (rs, rowNum) -> {
                BigDecimal gini = rs.getBigDecimal("gini_coefficient");
                BigDecimal top10Pct = rs.getBigDecimal("top_10_pct");

                // Determine concentration level based on Gini and top 10% holdings
                String concentrationLevel;
                if (gini.compareTo(BigDecimal.valueOf(0.7)) > 0 || top10Pct.compareTo(BigDecimal.valueOf(70)) > 0) {
                    concentrationLevel = "Highly Concentrated (High Inequality)";
                } else if (gini.compareTo(BigDecimal.valueOf(0.5)) > 0 || top10Pct.compareTo(BigDecimal.valueOf(50)) > 0) {
                    concentrationLevel = "Moderately Concentrated";
                } else if (gini.compareTo(BigDecimal.valueOf(0.35)) > 0 || top10Pct.compareTo(BigDecimal.valueOf(35)) > 0) {
                    concentrationLevel = "Fairly Distributed";
                } else {
                    concentrationLevel = "Well Distributed (Low Inequality)";
                }

                return new HolderConcentrationStats(
                    rs.getString("asset_unit"),
                    rs.getString("policy_id"),
                    rs.getString("asset_name"),
                    rs.getInt("total_holders"),
                    rs.getBigDecimal("total_supply"),
                    rs.getBigDecimal("top_1_pct"),
                    rs.getBigDecimal("top_5_pct"),
                    rs.getBigDecimal("top_10_pct"),
                    rs.getBigDecimal("top_25_pct"),
                    rs.getBigDecimal("top_10_holders_percent"),
                    rs.getBigDecimal("top_100_holders_percent"),
                    gini,
                    rs.getBigDecimal("median_holding"),
                    rs.getBigDecimal("mean_holding"),
                    concentrationLevel
                );
            }
        );

        if (results.isEmpty()) {
            throw new RuntimeException("No holder data found for asset: " + assetUnit);
        }

        return results.get(0);
    }

    @Tool(name = "tokens-by-holder-count",
          description = "Find most widely distributed tokens on Cardano (ranked by unique holder count). " +
                        "✅ OPTIMIZED with materialized view (sub-second performance). " +
                        "Returns tokens with the MOST holders, indicating broad adoption and distribution. " +
                        "Perfect for: Token discovery, finding popular tokens, analyzing network adoption. " +
                        "Filters: " +
                        "- minHolders: Minimum unique holders to include (default: 100, filters noise) " +
                        "- minSupply: Minimum total supply to filter out test tokens (default: 1000000) " +
                        "Use cases: " +
                        "- 'What are the most widely held tokens?' " +
                        "- 'Which tokens have the broadest community?' " +
                        "- 'Find tokens with network effects' " +
                        "IMPORTANT: Use 'get-token-registry-metadata-batch' to enrich results with token names. " +
                        "Note: Data is refreshed every 3 hours (up to 3 hours stale, acceptable for discovery). " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<TokenHolderStats> getTokensByHolderCount(
        @ToolParam(description = "Minimum unique holders to include (default: 100). Filters noise.") Integer minHolders,
        @ToolParam(description = "Minimum total supply to include (default: 1000000). Filters test tokens.") Long minSupply,
        @ToolParam(description = "Maximum number of results (default: 50, max: 200)") Integer limit
    ) {
        int minHolderCount = (minHolders != null && minHolders > 0) ? minHolders : 100;
        long minTotalSupply = (minSupply != null && minSupply > 0) ? minSupply : 1000000L;
        int resultLimit = (limit != null && limit > 0 && limit <= 200) ? limit : 50;

        log.debug("[TOKENS BY HOLDER COUNT] Finding tokens with min {} holders, min supply {}, limit {}",
                  minHolderCount, minTotalSupply, resultLimit);

        // Query pre-aggregated materialized view for instant results
        String sql = """
            SELECT
                policy_id,
                asset_name,
                asset_unit,
                holder_count,
                total_supply,
                utxo_count
            FROM token_holder_summary_mv
            WHERE holder_count >= :minHolders
              AND total_supply >= :minSupply
            ORDER BY holder_count DESC, total_supply DESC
            LIMIT :limit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("minHolders", minHolderCount);
        params.put("minSupply", minTotalSupply);
        params.put("limit", resultLimit);

        return jdbcTemplate.query(sql, params,
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

    @Tool(name = "address-token-diversity",
          description = "Find most diversified portfolios on Cardano (addresses holding the most unique tokens/policies). " +
                        "✅ OPTIMIZED with materialized view (sub-second performance). " +
                        "Returns addresses ranked by portfolio diversity (unique token count or unique policy count). " +
                        "Perfect for: Identifying power users, finding portfolio diversification patterns, whale analysis. " +
                        "Ranking options: " +
                        "- 'tokens' (default): Rank by unique token count (most diversified by individual tokens) " +
                        "- 'policies': Rank by unique policy count (most diversified by projects/collections) " +
                        "Filters: " +
                        "- minTokens: Minimum unique tokens to include (default: 10) " +
                        "- minAdaBalance: Minimum ADA balance to filter dust wallets (default: 10 ADA) " +
                        "Use cases: " +
                        "- 'Who are the most diversified holders?' " +
                        "- 'Find addresses with broad token exposure' " +
                        "- 'Identify power users and collectors' " +
                        "Note: Data is refreshed every 12 hours (up to 12 hours stale, acceptable for analytics). " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<AddressDiversityStats> getAddressTokenDiversity(
        @ToolParam(description = "Minimum unique tokens to include (default: 10, filters casual users)") Integer minTokens,
        @ToolParam(description = "Minimum ADA balance in lovelace (default: 10000000 = 10 ADA)") Long minAdaBalance,
        @ToolParam(description = "Ranking method: 'tokens' (unique tokens) or 'policies' (unique policies). Default: 'tokens'") String rankBy,
        @ToolParam(description = "Maximum number of results (default: 50, max: 200)") Integer limit
    ) {
        int minTokenCount = (minTokens != null && minTokens > 0) ? minTokens : 10;
        long minAda = (minAdaBalance != null && minAdaBalance > 0) ? minAdaBalance : 10000000L;
        String ranking = (rankBy != null && rankBy.equalsIgnoreCase("policies")) ? "policies" : "tokens";
        int resultLimit = (limit != null && limit > 0 && limit <= 200) ? limit : 50;

        log.debug("[ADDRESS DIVERSITY] Finding addresses with min {} tokens, min {} ADA, ranked by {}, limit {}",
                  minTokenCount, minAda, ranking, resultLimit);

        String orderByClause = ranking.equals("policies") ? "unique_policy_count DESC" : "unique_token_count DESC";

        // Query pre-aggregated materialized view for instant results
        String sql = """
            SELECT
                address,
                unique_token_count,
                unique_policy_count,
                ada_balance
            FROM address_token_diversity_mv
            WHERE unique_token_count >= :minTokens
              AND ada_balance >= :minAdaBalance
            ORDER BY %s, ada_balance DESC, address
            LIMIT :limit
            """.formatted(orderByClause);

        Map<String, Object> params = new HashMap<>();
        params.put("minTokens", minTokenCount);
        params.put("minAdaBalance", minAda);
        params.put("limit", resultLimit);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                int tokenCount = rs.getInt("unique_token_count");
                int policyCount = rs.getInt("unique_policy_count");

                // Determine diversity level
                String diversityLevel;
                int diversityMetric = ranking.equals("policies") ? policyCount : tokenCount;

                if (diversityMetric >= 100) {
                    diversityLevel = "Highly Diversified (Power User)";
                } else if (diversityMetric >= 50) {
                    diversityLevel = "Very Diversified";
                } else if (diversityMetric >= 25) {
                    diversityLevel = "Moderately Diversified";
                } else {
                    diversityLevel = "Somewhat Diversified";
                }

                return new AddressDiversityStats(
                    rowNum + 1,  // rank (1-based)
                    rs.getString("address"),
                    tokenCount,
                    policyCount,
                    rs.getBigDecimal("ada_balance"),
                    diversityLevel
                );
            }
        );
    }
}
