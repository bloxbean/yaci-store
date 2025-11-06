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
          description = "⚡ FAST balance lookup for address(es) - returns ALL assets (lovelace + tokens) without UTXO metadata. " +
                        "10-100x faster than utxo-balance-summary. Perfect for quick balance checks when you don't need UTXO count or first_seen metadata. " +
                        "IMPORTANT: For each token returned, use 'asset_unit' to fetch token registry metadata: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<asset_unit>.json " +
                        "Display as 'TokenName (TICKER): X.XXX' with proper decimals. " +
                        "Returns both lovelace and native tokens in a single fast query. " +
                        "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    public List<AssetHolding> getFastAddressBalance(
        @ToolParam(description = "Single address or comma-separated addresses") String addresses
    ) {
        log.debug("[FAST BALANCE] Getting all assets for addresses: {}", addresses);

        String sql = """
            SELECT
                unit as asset_unit,
                SUM(quantity) as quantity
            FROM address_balance_current
            WHERE address = ANY(:addresses)
              AND quantity > 0
            GROUP BY unit
            ORDER BY unit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("addresses", addresses.split(","));

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
}
