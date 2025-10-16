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
import java.util.List;
import java.util.Map;

/**
 * MCP service providing optimized UTXO aggregation queries.
 * Focuses on exact address matching with database-level aggregations for performance.
 *
 * Key Features:
 * - Exact address matching using = ANY(:addresses) for optimal index usage
 * - Point-in-time balance queries using spent_epoch/spent_at_slot
 * - JSONB asset handling with GIN index support
 * - Multi-address wallet support
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.utxo.enabled", "store.mcp-server.aggregation.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpUtxoAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "utxo-balance-summary",
          description = "Get current balance summary for address(es). " +
                        "Supports single address or multiple comma-separated addresses. " +
                        "Returns total lovelace, UTXO count, and active epochs. " +
                        "Automatically filters spent UTXOs for accurate balance.")
    public UtxoBalanceSummary getBalanceSummary(
        @ToolParam(description = "Single address or comma-separated addresses")
        String addresses
    ) {
        log.debug("Getting balance summary for addresses: {}", addresses);

        String sql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace,
                COUNT(DISTINCT epoch) as active_epochs,
                MIN(slot) as first_seen_slot,
                MAX(slot) as last_seen_slot
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' = 'lovelace'
              AND NOT EXISTS (
                  SELECT 1 FROM tx_input
                  WHERE tx_input.tx_hash = address_utxo.tx_hash
                  AND tx_input.output_index = address_utxo.output_index
              )
            """;

        return jdbcTemplate.queryForObject(sql,
            Map.of("addresses", addresses.split(",")),
            (rs, rowNum) -> new UtxoBalanceSummary(
                rs.getLong("utxo_count"),
                rs.getBigDecimal("total_lovelace"),
                rs.getInt("active_epochs"),
                rs.getLong("first_seen_slot"),
                rs.getLong("last_seen_slot")
            )
        );
    }

    @Tool(name = "utxo-balance-at-epoch",
          description = "Get UTXO balance as of a specific epoch (point-in-time query). " +
                        "Returns balance that existed at the END of that epoch. " +
                        "Filters UTXOs spent during or before the target epoch. " +
                        "Critical for historical balance analysis.")
    public HistoricalBalanceSummary getBalanceAtEpoch(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Target epoch number") int epoch
    ) {
        log.debug("Getting balance at epoch {} for addresses: {}", epoch, addresses);

        String sql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' = 'lovelace'
              AND epoch <= :targetEpoch
              AND (
                  -- Either unspent, or spent AFTER target epoch
                  NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                  OR EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                      AND tx_input.spent_epoch > :targetEpoch
                  )
              )
            """;

        return jdbcTemplate.queryForObject(sql,
            Map.of(
                "addresses", addresses.split(","),
                "targetEpoch", epoch
            ),
            (rs, rowNum) -> new HistoricalBalanceSummary(
                "epoch",
                epoch,
                rs.getLong("utxo_count"),
                rs.getBigDecimal("total_lovelace")
            )
        );
    }

    @Tool(name = "utxo-balance-at-slot",
          description = "Get UTXO balance as of a specific slot (precise point-in-time query). " +
                        "More precise than epoch-based query for intra-epoch analysis. " +
                        "Useful for exact moment balance calculations.")
    public HistoricalBalanceSummary getBalanceAtSlot(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Target slot number") long slot
    ) {
        log.debug("Getting balance at slot {} for addresses: {}", slot, addresses);

        String sql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' = 'lovelace'
              AND slot <= :targetSlot
              AND (
                  NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                  OR EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                      AND tx_input.spent_at_slot > :targetSlot
                  )
              )
            """;

        return jdbcTemplate.queryForObject(sql,
            Map.of(
                "addresses", addresses.split(","),
                "targetSlot", slot
            ),
            (rs, rowNum) -> new HistoricalBalanceSummary(
                "slot",
                slot,
                rs.getLong("utxo_count"),
                rs.getBigDecimal("total_lovelace")
            )
        );
    }

    @Tool(name = "balance-history-timeline",
          description = "Get balance timeline showing how balance changed over epoch range. " +
                        "Returns balance at the end of each epoch in the range. " +
                        "Perfect for portfolio tracking and visualization.")
    public List<BalanceHistoryPoint> getBalanceHistory(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch
    ) {
        log.debug("Getting balance history from epoch {} to {} for addresses: {}",
                  startEpoch, endEpoch, addresses);

        String sql = """
            WITH epoch_series AS (
                SELECT generate_series(:startEpoch, :endEpoch) as epoch
            )
            SELECT
                e.epoch,
                COUNT(u.tx_hash) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
            FROM epoch_series e
            LEFT JOIN address_utxo u ON u.epoch <= e.epoch
                                     AND u.owner_addr = ANY(:addresses)
            LEFT JOIN jsonb_array_elements(u.amounts) as elem ON elem->>'unit' = 'lovelace'
            WHERE (
                u.owner_addr IS NULL
                OR (
                    NOT EXISTS (
                        SELECT 1 FROM tx_input ti
                        WHERE ti.tx_hash = u.tx_hash
                        AND ti.output_index = u.output_index
                    )
                    OR EXISTS (
                        SELECT 1 FROM tx_input ti
                        WHERE ti.tx_hash = u.tx_hash
                        AND ti.output_index = u.output_index
                        AND ti.spent_epoch > e.epoch
                    )
                )
            )
            GROUP BY e.epoch
            ORDER BY e.epoch
            """;

        return jdbcTemplate.query(sql,
            Map.of(
                "addresses", addresses.split(","),
                "startEpoch", startEpoch,
                "endEpoch", endEpoch
            ),
            (rs, rowNum) -> new BalanceHistoryPoint(
                rs.getInt("epoch"),
                rs.getLong("utxo_count"),
                rs.getBigDecimal("total_lovelace")
            )
        );
    }

    @Tool(name = "asset-balance-by-address",
          description = "Get balance of a specific asset for address(es). " +
                        "Asset format: policyId + assetName (hex). " +
                        "Returns holder count and total quantity.")
    public AssetBalanceSummary getAssetBalance(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit
    ) {
        log.debug("Getting asset balance for asset {} and addresses: {}", assetUnit, addresses);

        String sql = """
            SELECT
                COUNT(DISTINCT owner_addr) as holder_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_quantity
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' = :assetUnit
              AND NOT EXISTS (
                  SELECT 1 FROM tx_input
                  WHERE tx_input.tx_hash = address_utxo.tx_hash
                  AND tx_input.output_index = address_utxo.output_index
              )
            """;

        return jdbcTemplate.queryForObject(sql,
            Map.of("addresses", addresses.split(","), "assetUnit", assetUnit),
            (rs, rowNum) -> new AssetBalanceSummary(
                assetUnit,
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_quantity")
            )
        );
    }

    @Tool(name = "stake-address-portfolio",
          description = "Get complete portfolio for a stake address including all assets. " +
                        "Returns ADA balance, asset holdings, and UTXO distribution across all delegated addresses.")
    public StakeAddressPortfolio getStakeAddressPortfolio(
        @ToolParam(description = "Stake address (stake1...)") String stakeAddress
    ) {
        log.debug("Getting portfolio for stake address: {}", stakeAddress);

        // Query 1: Total ADA and UTXO count
        String adaSql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_stake_addr = :stakeAddress
              AND elem->>'unit' = 'lovelace'
              AND NOT EXISTS (
                  SELECT 1 FROM tx_input
                  WHERE tx_input.tx_hash = address_utxo.tx_hash
                  AND tx_input.output_index = address_utxo.output_index
              )
            """;

        Map<String, Object> adaResult = jdbcTemplate.queryForMap(adaSql,
            Map.of("stakeAddress", stakeAddress));

        // Query 2: All assets (excluding lovelace)
        String assetsSql = """
            SELECT
                elem->>'unit' as asset_unit,
                elem->>'policy_id' as policy_id,
                elem->>'asset_name' as asset_name,
                SUM((elem->>'quantity')::bigint) as quantity
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_stake_addr = :stakeAddress
              AND elem->>'unit' != 'lovelace'
              AND NOT EXISTS (
                  SELECT 1 FROM tx_input
                  WHERE tx_input.tx_hash = address_utxo.tx_hash
                  AND tx_input.output_index = address_utxo.output_index
              )
            GROUP BY elem->>'unit', elem->>'policy_id', elem->>'asset_name'
            ORDER BY quantity DESC
            """;

        List<AssetHolding> assets = jdbcTemplate.query(assetsSql,
            Map.of("stakeAddress", stakeAddress),
            (rs, rowNum) -> new AssetHolding(
                rs.getString("asset_unit"),
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getBigDecimal("quantity")
            )
        );

        return new StakeAddressPortfolio(
            stakeAddress,
            ((Number) adaResult.get("utxo_count")).longValue(),
            new BigDecimal(adaResult.get("total_lovelace").toString()),
            assets
        );
    }

    @Tool(name = "asset-balance-at-epoch",
          description = "Get asset balance as of a specific epoch (point-in-time query). " +
                        "Returns quantity of specific asset that existed at the END of that epoch. " +
                        "Filters UTXOs spent during or before the target epoch. " +
                        "Essential for historical asset holding analysis.")
    public HistoricalBalanceSummary getAssetBalanceAtEpoch(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit,
        @ToolParam(description = "Target epoch number") int epoch
    ) {
        log.debug("Getting asset {} balance at epoch {} for addresses: {}", assetUnit, epoch, addresses);

        String sql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_quantity
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' = :assetUnit
              AND epoch <= :targetEpoch
              AND (
                  -- Either unspent, or spent AFTER target epoch
                  NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                  OR EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                      AND tx_input.spent_epoch > :targetEpoch
                  )
              )
            """;

        return jdbcTemplate.queryForObject(sql,
            Map.of(
                "addresses", addresses.split(","),
                "assetUnit", assetUnit,
                "targetEpoch", epoch
            ),
            (rs, rowNum) -> new HistoricalBalanceSummary(
                "epoch",
                epoch,
                rs.getLong("utxo_count"),
                rs.getBigDecimal("total_quantity")
            )
        );
    }

    @Tool(name = "multi-asset-summary",
          description = "Get summary of all assets held by address(es). " +
                        "Returns list of all native tokens and NFTs with quantities. " +
                        "Excludes lovelace - use utxo-balance-summary for ADA. " +
                        "Perfect for portfolio overview and asset discovery.")
    public List<AssetHolding> getMultiAssetSummary(
        @ToolParam(description = "Address or comma-separated addresses") String addresses
    ) {
        log.debug("Getting multi-asset summary for addresses: {}", addresses);

        String sql = """
            SELECT
                elem->>'unit' as asset_unit,
                elem->>'policy_id' as policy_id,
                elem->>'asset_name' as asset_name,
                SUM((elem->>'quantity')::bigint) as quantity
            FROM address_utxo,
                 jsonb_array_elements(amounts) as elem
            WHERE owner_addr = ANY(:addresses)
              AND elem->>'unit' != 'lovelace'
              AND NOT EXISTS (
                  SELECT 1 FROM tx_input
                  WHERE tx_input.tx_hash = address_utxo.tx_hash
                  AND tx_input.output_index = address_utxo.output_index
              )
            GROUP BY elem->>'unit', elem->>'policy_id', elem->>'asset_name'
            ORDER BY quantity DESC
            """;

        return jdbcTemplate.query(sql,
            Map.of("addresses", addresses.split(",")),
            (rs, rowNum) -> new AssetHolding(
                rs.getString("asset_unit"),
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getBigDecimal("quantity")
            )
        );
    }
}
