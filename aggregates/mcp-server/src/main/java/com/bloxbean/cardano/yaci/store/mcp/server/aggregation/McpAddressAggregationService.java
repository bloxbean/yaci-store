package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressFirstSeen;
import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressNFTPortfolio;
import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressPortfolio;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DelegationEvent;
import com.bloxbean.cardano.yaci.store.mcp.server.model.NFT;
import com.bloxbean.cardano.yaci.store.mcp.server.model.NFTCollection;
import com.bloxbean.cardano.yaci.store.mcp.server.model.StakingHistory;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TokenBalance;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service providing address wallet intelligence and portfolio analytics.
 * Focuses on address balance tracking, portfolio summaries, and transaction history.
 *
 * Key Features:
 * - Complete portfolio view (ADA + native tokens + NFTs)
 * - Address transaction history with net transfers
 * - Staking delegation history and rewards
 * - NFT collection management
 *
 * Use Cases:
 * - Wallet applications and portfolio tracking
 * - Address behavior analysis
 * - Asset discovery and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.utxo.enabled", "store.mcp-server.aggregation.address.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpAddressAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final BigInteger ONE_ADA = BigInteger.valueOf(1_000_000); // 1 ADA = 1,000,000 lovelace

    @Tool(name = "address-portfolio-summary",
          description = "Get complete portfolio for an address: ADA balance + all native tokens + NFTs. " +
                        "Returns BOTH lovelace and ADA units to prevent confusion. " +
                        "All token quantities are in whole units (no decimals). " +
                        "Only includes unspent UTXOs (current holdings). " +
                        "Essential for wallet apps and portfolio tracking.")
    public AddressPortfolio getAddressPortfolioSummary(
        @ToolParam(description = "Cardano address (addr1... or addr_test...)") String address
    ) {
        log.debug("Getting portfolio summary for address: {}", address);

        // Query to get ADA balance and UTXO count from unspent UTXOs
        String adaQuery = """
            SELECT
                COALESCE(SUM(u.lovelace_amount), 0) as total_lovelace,
                COUNT(*) as utxo_count
            FROM address_utxo u
            LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
            WHERE ti.tx_hash IS NULL
              AND u.owner_addr = :address
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);

        // Get ADA balance and UTXO count
        Map<String, Object> adaResult = jdbcTemplate.queryForMap(adaQuery, params);
        BigInteger totalLovelace = new BigInteger(adaResult.get("total_lovelace").toString());
        int utxoCount = ((Number) adaResult.get("utxo_count")).intValue();

        // Convert lovelace to ADA (divide by 1,000,000)
        BigDecimal totalAda = new BigDecimal(totalLovelace)
            .divide(new BigDecimal(ONE_ADA), 6, RoundingMode.HALF_UP);

        // Query to aggregate native tokens from unspent UTXOs
        String tokenQuery = """
            WITH unspent_utxos AS (
                SELECT u.amounts
                FROM address_utxo u
                LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                WHERE ti.tx_hash IS NULL
                  AND u.owner_addr = :address
                  AND u.amounts IS NOT NULL
            )
            SELECT
                (elem->>'unit') as unit,
                (elem->>'policy_id') as policy_id,
                (elem->>'asset_name') as asset_name,
                SUM((elem->>'quantity')::numeric) as total_quantity
            FROM unspent_utxos
            CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
            WHERE (elem->>'unit') != 'lovelace'
            GROUP BY unit, policy_id, asset_name
            ORDER BY unit
            """;

        List<TokenBalance> nativeTokens = jdbcTemplate.query(tokenQuery, params,
            (rs, rowNum) -> {
                String unit = rs.getString("unit");
                String policyId = rs.getString("policy_id");
                String assetNameHex = rs.getString("asset_name");
                BigInteger quantity = rs.getBigDecimal("total_quantity").toBigInteger();

                // Try to decode asset name from hex to UTF-8
                String assetName = decodeAssetName(assetNameHex);

                return new TokenBalance(
                    unit,
                    policyId,
                    assetName,
                    assetNameHex,
                    quantity
                );
            }
        );

        // Count NFTs (tokens with quantity = 1)
        int nftCount = (int) nativeTokens.stream()
            .filter(token -> token.quantity().equals(BigInteger.ONE))
            .count();

        return new AddressPortfolio(
            address,
            totalLovelace,
            totalAda,
            nativeTokens,
            nftCount,
            utxoCount
        );
    }

    /**
     * Decode asset name from hex to UTF-8 string.
     * Falls back to hex if not valid UTF-8.
     */
    private String decodeAssetName(String hexAssetName) {
        if (hexAssetName == null || hexAssetName.isEmpty()) {
            return "";
        }

        try {
            // Convert hex string to bytes
            byte[] bytes = hexStringToByteArray(hexAssetName);
            // Try to decode as UTF-8
            String decoded = new String(bytes, StandardCharsets.UTF_8);

            // Check if the decoded string contains only printable characters
            if (decoded.chars().allMatch(c -> c >= 32 && c < 127)) {
                return decoded;
            } else {
                // Return hex if contains non-printable characters
                return hexAssetName;
            }
        } catch (Exception e) {
            // Fall back to hex on any error
            return hexAssetName;
        }
    }

    /**
     * Convert hex string to byte array.
     */
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    @Tool(name = "address-first-seen",
          description = "Get when an address first appeared on-chain. " +
                        "Returns first slot, block, timestamp, and age analysis. " +
                        "Useful for wallet age analysis, address behavior profiling, " +
                        "and identifying new vs old holders. " +
                        "Essential for security analysis and user segmentation.")
    public AddressFirstSeen getAddressFirstSeen(
        @ToolParam(description = "Cardano address (addr1... or addr_test...)") String address
    ) {
        log.debug("Getting first-seen info for address: {}", address);

        String query = """
            SELECT
                MIN(u.slot) as first_slot,
                MIN(u.block) as first_block,
                MIN(u.block_time) as first_block_time,
                MIN(u.epoch) as first_epoch
            FROM address_utxo u
            WHERE u.owner_addr = :address
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, params);

            Long firstSlot = result.get("first_slot") != null
                ? ((Number) result.get("first_slot")).longValue()
                : null;
            Long firstBlock = result.get("first_block") != null
                ? ((Number) result.get("first_block")).longValue()
                : null;
            Long firstBlockTime = result.get("first_block_time") != null
                ? ((Number) result.get("first_block_time")).longValue()
                : null;
            Integer firstEpoch = result.get("first_epoch") != null
                ? ((Number) result.get("first_epoch")).intValue()
                : null;

            // Return null values if address never appeared on-chain
            if (firstSlot == null) {
                return AddressFirstSeen.create(address, null, null, null, null);
            }

            return AddressFirstSeen.create(
                address,
                firstSlot,
                firstBlock,
                firstBlockTime,
                firstEpoch
            );
        } catch (Exception e) {
            log.warn("No data found for address: {}", address);
            return AddressFirstSeen.create(address, null, null, null, null);
        }
    }

    @Tool(name = "address-staking-history",
          description = "Get complete staking history for a stake address. " +
                        "Returns delegation changes over time and total rewards earned. " +
                        "Shows current pool delegation and registration status. " +
                        "All reward amounts in lovelace (BigInteger) to prevent confusion. " +
                        "Essential for analyzing staking behavior and reward tracking.")
    public StakingHistory getAddressStakingHistory(
        @ToolParam(description = "Cardano stake address (stake1... or stake_test...)") String stakeAddress
    ) {
        log.debug("Getting staking history for stake address: {}", stakeAddress);

        // Query delegation history
        String delegationQuery = """
            SELECT
                d.tx_hash,
                d.cert_index,
                d.pool_id,
                d.epoch,
                d.slot,
                d.block_time
            FROM delegation d
            WHERE d.address = :stakeAddress
            ORDER BY d.slot ASC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("stakeAddress", stakeAddress);

        List<DelegationEvent> delegationHistory = jdbcTemplate.query(delegationQuery, params,
            (rs, rowNum) -> DelegationEvent.create(
                rs.getString("tx_hash"),
                rs.getInt("cert_index"),
                rs.getString("pool_id"),
                rs.getInt("epoch"),
                rs.getLong("slot"),
                rs.getLong("block_time")
            )
        );

        // Get current pool (most recent delegation)
        String currentPoolId = delegationHistory.isEmpty() ? null
            : delegationHistory.get(delegationHistory.size() - 1).poolId();

        // Query total rewards
        String rewardQuery = """
            SELECT COALESCE(SUM(r.amount), 0) as total_rewards
            FROM reward r
            WHERE r.address = :stakeAddress
            """;

        BigInteger totalRewards = jdbcTemplate.queryForObject(rewardQuery, params,
            (rs, rowNum) -> rs.getBigDecimal("total_rewards").toBigInteger()
        );

        // Check if currently registered
        String registrationQuery = """
            SELECT COUNT(*) as reg_count
            FROM stake_registration
            WHERE address = :stakeAddress
              AND type = 'REGISTRATION'
            """;

        String deregistrationQuery = """
            SELECT COUNT(*) as dereg_count
            FROM stake_registration
            WHERE address = :stakeAddress
              AND type = 'DEREGISTRATION'
            """;

        Integer regCount = jdbcTemplate.queryForObject(registrationQuery, params,
            (rs, rowNum) -> rs.getInt("reg_count")
        );

        Integer deregCount = jdbcTemplate.queryForObject(deregistrationQuery, params,
            (rs, rowNum) -> rs.getInt("dereg_count")
        );

        // Currently registered if registrations > deregistrations
        boolean isRegistered = (regCount != null && deregCount != null) && (regCount > deregCount);

        return StakingHistory.create(
            stakeAddress,
            currentPoolId,
            delegationHistory,
            totalRewards != null ? totalRewards : BigInteger.ZERO,
            isRegistered
        );
    }

    @Tool(name = "address-nft-collection",
          description = "Get all NFTs owned by an address, grouped by collection (policy ID). " +
                        "NFTs are tokens with quantity = 1. " +
                        "Shows collection count, NFT count per collection, and individual NFT details. " +
                        "Only includes unspent UTXOs (current holdings). " +
                        "Essential for NFT galleries, collection tracking, and portfolio visualization.")
    public AddressNFTPortfolio getAddressNFTCollection(
        @ToolParam(description = "Cardano address (addr1... or addr_test...)") String address
    ) {
        log.debug("Getting NFT collection for address: {}", address);

        // Query to get all NFTs (tokens with quantity = 1) from unspent UTXOs
        String nftQuery = """
            WITH unspent_utxos AS (
                SELECT u.amounts
                FROM address_utxo u
                LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                WHERE ti.tx_hash IS NULL
                  AND u.owner_addr = :address
                  AND u.amounts IS NOT NULL
            )
            SELECT
                (elem->>'unit') as unit,
                (elem->>'policy_id') as policy_id,
                (elem->>'asset_name') as asset_name,
                (elem->>'quantity')::numeric as quantity
            FROM unspent_utxos
            CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
            WHERE (elem->>'unit') != 'lovelace'
              AND (elem->>'quantity')::numeric = 1
            ORDER BY policy_id, unit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);

        List<NFT> allNFTs = jdbcTemplate.query(nftQuery, params,
            (rs, rowNum) -> {
                String unit = rs.getString("unit");
                String policyId = rs.getString("policy_id");
                String assetNameHex = rs.getString("asset_name");
                BigInteger quantity = rs.getBigDecimal("quantity").toBigInteger();

                // Decode asset name from hex to UTF-8
                String assetName = decodeAssetName(assetNameHex);

                return new NFT(
                    unit,
                    policyId,
                    assetName,
                    assetNameHex,
                    quantity
                );
            }
        );

        // Group NFTs by policy ID (collection)
        Map<String, List<NFT>> nftsByPolicyId = new HashMap<>();
        for (NFT nft : allNFTs) {
            nftsByPolicyId.computeIfAbsent(nft.policyId(), k -> new ArrayList<>()).add(nft);
        }

        // Create NFTCollection objects
        List<NFTCollection> collections = nftsByPolicyId.entrySet().stream()
            .map(entry -> new NFTCollection(
                entry.getKey(),
                entry.getValue().size(),
                entry.getValue()
            ))
            .toList();

        return new AddressNFTPortfolio(
            address,
            allNFTs.size(),
            collections.size(),
            collections
        );
    }
}
