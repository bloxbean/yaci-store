package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.client.address.Address;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Extract payment credential hashes from Cardano addresses.
     * Used for Franken address queries (same payment credential, different stake credentials).
     *
     * @param addresses Comma-separated list of Cardano addresses
     * @return List of payment credential hashes in hex format
     */
    private List<String> extractPaymentCredentials(String addresses) {
        String[] addressArray = addresses.split(",");
        List<String> credentials = new ArrayList<>();

        for (String addr : addressArray) {
            try {
                Address address = new Address(addr.trim());
                Optional<byte[]> paymentCredHash = address.getPaymentCredentialHash();

                if (paymentCredHash.isPresent()) {
                    String credentialHex = HexUtil.encodeHexString(paymentCredHash.get());
                    credentials.add(credentialHex);
                    log.debug("Extracted payment credential {} from address {}", credentialHex, addr);
                } else {
                    log.warn("No payment credential found for address: {}", addr);
                }
            } catch (Exception e) {
                log.error("Failed to extract payment credential from address: {}", addr, e);
            }
        }

        return credentials;
    }

    @Tool(name = "utxo-balance-summary",
          description = "Get current balance summary for address(es). " +
                        "Supports single address or multiple comma-separated addresses. " +
                        "Returns total lovelace, UTXO count, and active epochs. " +
                        "Automatically filters spent UTXOs for accurate balance. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public UtxoBalanceSummary getBalanceSummary(
        @ToolParam(description = "Single address or comma-separated addresses")
        String addresses,

        @ToolParam(description = "If true, aggregate UTXOs by payment credential (finds all addresses with same payment key, useful for Franken addresses). Default: false (exact address match)")
        Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting balance summary for addresses: {}, searchByPaymentCredential: {}",
                  addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace,
                    COUNT(DISTINCT epoch) as active_epochs,
                    MIN(slot) as first_seen_slot,
                    MAX(slot) as last_seen_slot
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
                  AND elem->>'unit' = 'lovelace'
                  AND NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                """;
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
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
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.queryForObject(sql, params,
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
                        "Critical for historical balance analysis. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public HistoricalBalanceSummary getBalanceAtEpoch(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Target epoch number") int epoch,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting balance at epoch {} for addresses: {}, searchByPaymentCredential: {}",
                  epoch, addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("targetEpoch", epoch);

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
                  AND elem->>'unit' = 'lovelace'
                  AND epoch <= :targetEpoch
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
                          AND tx_input.spent_epoch > :targetEpoch
                      )
                  )
                """;
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_addr = ANY(:addresses)
                  AND elem->>'unit' = 'lovelace'
                  AND epoch <= :targetEpoch
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
                          AND tx_input.spent_epoch > :targetEpoch
                      )
                  )
                """;
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.queryForObject(sql, params,
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
                        "Useful for exact moment balance calculations. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public HistoricalBalanceSummary getBalanceAtSlot(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Target slot number") long slot,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting balance at slot {} for addresses: {}, searchByPaymentCredential: {}",
                  slot, addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("targetSlot", slot);

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
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
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
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
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.queryForObject(sql, params,
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
                        "Perfect for portfolio tracking and visualization. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public List<BalanceHistoryPoint> getBalanceHistory(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Start epoch") int startEpoch,
        @ToolParam(description = "End epoch") int endEpoch,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting balance history from epoch {} to {} for addresses: {}, searchByPaymentCredential: {}",
                  startEpoch, endEpoch, addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("startEpoch", startEpoch);
        params.put("endEpoch", endEpoch);

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                WITH epoch_series AS (
                    SELECT generate_series(:startEpoch, :endEpoch) as epoch
                )
                SELECT
                    e.epoch,
                    COUNT(u.tx_hash) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_lovelace
                FROM epoch_series e
                LEFT JOIN address_utxo u ON u.epoch <= e.epoch
                                         AND u.owner_payment_credential = ANY(:credentials)
                LEFT JOIN jsonb_array_elements(u.amounts) as elem ON elem->>'unit' = 'lovelace'
                WHERE (
                    u.owner_payment_credential IS NULL
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
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
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
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.query(sql, params,
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
                        "Returns holder count and total quantity. " +
                        "IMPORTANT: Use the 'assetUnit' parameter to fetch token metadata from: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<assetUnit>.json " +
                        "Present balance as 'You hold X.XXX TokenName (TICKER)' using decimals for proper display. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public AssetBalanceSummary getAssetBalance(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting asset balance for asset {} and addresses: {}, searchByPaymentCredential: {}",
                  assetUnit, addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("assetUnit", assetUnit);

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    COUNT(DISTINCT owner_payment_credential) as holder_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_quantity
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
                  AND elem->>'unit' = :assetUnit
                  AND NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                """;
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
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
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.queryForObject(sql, params,
            (rs, rowNum) -> new AssetBalanceSummary(
                assetUnit,
                rs.getInt("holder_count"),
                rs.getBigDecimal("total_quantity")
            )
        );
    }

    @Tool(name = "stake-address-portfolio",
          description = "Get complete portfolio for a stake address including all assets. " +
                        "Returns ADA balance, asset holdings, and UTXO distribution across all delegated addresses. " +
                        "IMPORTANT: For each asset in the portfolio, fetch token registry metadata using asset_unit: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<asset_unit>.json " +
                        "Create a complete portfolio view showing 'ADA: X.XXX, TokenName (TICKER): Y.YYY, ...' with all tokens identified by name.")
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
                        "IMPORTANT: Fetch token registry metadata using 'assetUnit' parameter to provide historical context: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<assetUnit>.json " +
                        "Present as 'At epoch X, held Y.YYY TokenName (TICKER)' for clear historical analysis. " +
                        "Essential for historical asset holding analysis. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public HistoricalBalanceSummary getAssetBalanceAtEpoch(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "Asset unit (policyId + assetName in hex)") String assetUnit,
        @ToolParam(description = "Target epoch number") int epoch,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting asset {} balance at epoch {} for addresses: {}, searchByPaymentCredential: {}",
                  assetUnit, epoch, addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("assetUnit", assetUnit);
        params.put("targetEpoch", epoch);

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_quantity
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
                  AND elem->>'unit' = :assetUnit
                  AND epoch <= :targetEpoch
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
                          AND tx_input.spent_epoch > :targetEpoch
                      )
                  )
                """;
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
                SELECT
                    COUNT(*) as utxo_count,
                    COALESCE(SUM((elem->>'quantity')::bigint), 0) as total_quantity
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_addr = ANY(:addresses)
                  AND elem->>'unit' = :assetUnit
                  AND epoch <= :targetEpoch
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
                          AND tx_input.spent_epoch > :targetEpoch
                      )
                  )
                """;
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.queryForObject(sql, params,
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
                        "IMPORTANT: For each asset returned, use 'asset_unit' to fetch token registry metadata: " +
                        "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<asset_unit>.json " +
                        "Display holdings as 'X.XXX TokenName (TICKER)' using decimals field for proper formatting. " +
                        "This transforms raw portfolio data into human-readable token names for better UX. " +
                        "Excludes lovelace - use utxo-balance-summary for ADA. " +
                        "Perfect for portfolio overview and asset discovery. " +
                        "Supports Franken address search via searchByPaymentCredential=true to aggregate balances " +
                        "across all addresses sharing the same payment credential but different stake credentials.")
    public List<AssetHolding> getMultiAssetSummary(
        @ToolParam(description = "Address or comma-separated addresses") String addresses,
        @ToolParam(description = "If true, aggregate by payment credential (Franken addresses). Default: false") Boolean searchByPaymentCredential
    ) {
        boolean usePaymentCredential = (searchByPaymentCredential != null && searchByPaymentCredential);
        log.debug("Getting multi-asset summary for addresses: {}, searchByPaymentCredential: {}",
                  addresses, usePaymentCredential);

        String sql;
        Map<String, Object> params = new HashMap<>();

        if (usePaymentCredential) {
            List<String> credentials = extractPaymentCredentials(addresses);
            sql = """
                SELECT
                    elem->>'unit' as asset_unit,
                    elem->>'policy_id' as policy_id,
                    elem->>'asset_name' as asset_name,
                    SUM((elem->>'quantity')::bigint) as quantity
                FROM address_utxo,
                     jsonb_array_elements(amounts) as elem
                WHERE owner_payment_credential = ANY(:credentials)
                  AND elem->>'unit' != 'lovelace'
                  AND NOT EXISTS (
                      SELECT 1 FROM tx_input
                      WHERE tx_input.tx_hash = address_utxo.tx_hash
                      AND tx_input.output_index = address_utxo.output_index
                  )
                GROUP BY elem->>'unit', elem->>'policy_id', elem->>'asset_name'
                ORDER BY quantity DESC
                """;
            params.put("credentials", credentials.toArray(new String[0]));
        } else {
            sql = """
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
            params.put("addresses", addresses.split(","));
        }

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new AssetHolding(
                rs.getString("asset_unit"),
                rs.getString("policy_id"),
                rs.getString("asset_name"),
                rs.getBigDecimal("quantity")
            )
        );
    }
}
