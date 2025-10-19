package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.TokenHolderDistribution;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TopHolder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service providing advanced token analytics and market intelligence.
 * Focuses on token distribution analysis, whale watching, and trading activity.
 *
 * Key Features:
 * - Token holder distribution analysis (whale detection)
 * - Top holder identification with concentration metrics
 * - Trending tokens by transaction volume
 * - Market intelligence and token activity tracking
 *
 * Use Cases:
 * - Whale watching and large holder analysis
 * - Token distribution fairness assessment
 * - Hot token discovery by trading volume
 * - Market intelligence for traders and analysts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.utxo.enabled", "store.mcp-server.aggregation.token-analytics.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpTokenAnalyticsService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "token-holder-distribution",
          description = "Analyze token holder distribution with whale detection and concentration metrics. " +
                        "Returns holder count, top holders list, and Gini coefficient for inequality analysis. " +
                        "All quantities in whole units (BigInteger) to prevent LLM confusion. " +
                        "Gini coefficient: 0 = perfect equality, 1 = perfect inequality (>0.5 = high concentration). " +
                        "Top 10 concentration shows % of supply held by largest holders (whale indicator). " +
                        "Essential for: whale watching, token fairness analysis, market intelligence. " +
                        "Only counts currently unspent UTXOs (active holdings).")
    public TokenHolderDistribution getTokenHolderDistribution(
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit,
        @ToolParam(description = "Number of top holders to return (default: 10, max: 50)") Integer topN
    ) {
        log.debug("Analyzing holder distribution for asset: {}", assetUnit);

        int effectiveTopN = (topN != null && topN > 0) ? Math.min(topN, 50) : 10;

        // First, get basic token info from token_holder_summary
        String tokenInfoQuery = """
            SELECT
                policy_id,
                asset_name,
                asset_unit,
                holder_count,
                total_supply
            FROM token_holder_summary
            WHERE asset_unit = :assetUnit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("assetUnit", assetUnit);

        List<Map<String, Object>> tokenInfo = jdbcTemplate.queryForList(tokenInfoQuery, params);

        if (tokenInfo.isEmpty()) {
            throw new RuntimeException("No holder data found for asset: " + assetUnit);
        }

        Map<String, Object> info = tokenInfo.get(0);
        String policyId = (String) info.get("policy_id");
        String assetName = (String) info.get("asset_name");
        int totalHolders = ((Number) info.get("holder_count")).intValue();
        BigDecimal totalSupply = (BigDecimal) info.get("total_supply");

        // Query top holders from address_utxo
        String topHoldersQuery = """
            WITH unspent_utxos AS (
                SELECT u.owner_addr, u.amounts
                FROM address_utxo u
                LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                WHERE ti.tx_hash IS NULL
                  AND u.amounts IS NOT NULL
            ),
            holder_balances AS (
                SELECT
                    owner_addr,
                    SUM((elem->>'quantity')::numeric) as total_quantity
                FROM unspent_utxos
                CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
                WHERE (elem->>'unit') = :assetUnit
                GROUP BY owner_addr
            )
            SELECT
                owner_addr,
                total_quantity
            FROM holder_balances
            ORDER BY total_quantity DESC
            LIMIT :topN
            """;

        params.put("topN", effectiveTopN);

        List<TopHolder> topHolders = new ArrayList<>();
        BigDecimal totalSupplyForCalc = totalSupply.compareTo(BigDecimal.ZERO) > 0
            ? totalSupply
            : BigDecimal.ONE; // Prevent division by zero

        List<Map<String, Object>> holderResults = jdbcTemplate.queryForList(topHoldersQuery, params);

        int rank = 1;
        for (Map<String, Object> row : holderResults) {
            String address = (String) row.get("owner_addr");
            BigDecimal quantity = (BigDecimal) row.get("total_quantity");
            BigInteger quantityInt = quantity.toBigInteger();

            // Calculate percentage of supply
            BigDecimal percentage = quantity
                .divide(totalSupplyForCalc, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            topHolders.add(new TopHolder(address, quantityInt, percentage, rank));
            rank++;
        }

        return TokenHolderDistribution.create(
            assetUnit,
            policyId,
            assetName,
            totalHolders,
            totalSupply.toBigInteger(),
            topHolders
        );
    }
}
