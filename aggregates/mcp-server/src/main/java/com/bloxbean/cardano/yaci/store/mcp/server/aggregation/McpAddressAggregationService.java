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
          description = "Get complete portfolio for an address: ADA balance + native tokens + NFTs. " +
                        "Returns BOTH lovelace and ADA units to prevent confusion. " +
                        "⚠️ CONTEXT MANAGEMENT: By default returns 20 tokens to conserve context. " +
                        "RECOMMENDED WORKFLOW: " +
                        "(1) First call: Use maxTokens=10 for quick overview. " +
                        "(2) If user wants more detail: maxTokens=30. " +
                        "(3) If user wants comprehensive view: maxTokens=50-100. " +
                        "(4) Analyzing multiple addresses: Keep maxTokens=10 per address. " +
                        "Use tokenSort='random' for variety to discover different tokens each call. " +
                        "Use skipTokens=true for ADA-only analysis (much faster, no token data). " +
                        "All token quantities are in whole units (no decimals). " +
                        "Only includes unspent UTXOs (current holdings). " +
                        "Essential for wallet apps and portfolio tracking.")
    public AddressPortfolio getAddressPortfolioSummary(
        @ToolParam(description = "Cardano address (addr1... or addr_test...)") String address,
        @ToolParam(description = "Skip token analysis entirely (default: false). Set true for ADA-only, much faster queries.") Boolean skipTokens,
        @ToolParam(description = "Token sort method: 'quantity' (default, show top holdings first) or 'random' (random selection for variety)") String tokenSort,
        @ToolParam(description = "Max tokens to return (default: 20, max: 100, min: 1). " +
                                 "⚠️ STRATEGY: Start with 10-20 for overview, increase only if user needs more detail. " +
                                 "Each token ~50 chars, so 50 tokens = ~2.5KB context.") Integer maxTokens
    ) {
        // Normalize parameters
        boolean shouldSkipTokens = skipTokens != null && skipTokens;
        String effectiveTokenSort = (tokenSort != null && tokenSort.equalsIgnoreCase("random")) ? "random" : "quantity";

        // Normalize and validate maxTokens: default 20, min 1, max 100
        int effectiveMaxTokens = 20; // Default
        if (maxTokens != null) {
            if (maxTokens < 1) {
                effectiveMaxTokens = 1;
                log.warn("maxTokens {} is below minimum, using 1", maxTokens);
            } else if (maxTokens > 100) {
                effectiveMaxTokens = 100;
                log.warn("maxTokens {} exceeds maximum, using 100", maxTokens);
            } else {
                effectiveMaxTokens = maxTokens;
            }
        }

        log.debug("Getting portfolio summary for address: {}, skipTokens: {}, tokenSort: {}, maxTokens: {}",
                  address, shouldSkipTokens, effectiveTokenSort, effectiveMaxTokens);

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

        // Initialize token-related variables
        List<TokenBalance> nativeTokens = new java.util.ArrayList<>();
        int totalTokenCount = 0;
        int nftCount = 0;
        String message = null;

        if (shouldSkipTokens) {
            // Count tokens but don't fetch them
            String tokenCountQuery = """
                WITH unspent_utxos AS (
                    SELECT u.amounts
                    FROM address_utxo u
                    LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                    WHERE ti.tx_hash IS NULL
                      AND u.owner_addr = :address
                      AND u.amounts IS NOT NULL
                )
                SELECT COUNT(DISTINCT (elem->>'unit')) as token_count
                FROM unspent_utxos
                CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
                WHERE (elem->>'unit') != 'lovelace'
                """;

            Map<String, Object> countResult = jdbcTemplate.queryForMap(tokenCountQuery, params);
            totalTokenCount = ((Number) countResult.get("token_count")).intValue();

            message = String.format("⚠️ Token analysis skipped. Address holds %d unique tokens. Set skipTokens=false to include tokens.",
                                  totalTokenCount);

            log.debug("Skipped token analysis. Total token count: {}", totalTokenCount);

        } else {
            // First, count total unique tokens
            String tokenCountQuery = """
                WITH unspent_utxos AS (
                    SELECT u.amounts
                    FROM address_utxo u
                    LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                    WHERE ti.tx_hash IS NULL
                      AND u.owner_addr = :address
                      AND u.amounts IS NOT NULL
                )
                SELECT COUNT(DISTINCT (elem->>'unit')) as token_count
                FROM unspent_utxos
                CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
                WHERE (elem->>'unit') != 'lovelace'
                """;

            Map<String, Object> countResult = jdbcTemplate.queryForMap(tokenCountQuery, params);
            totalTokenCount = ((Number) countResult.get("token_count")).intValue();

            // Then, fetch tokens with appropriate ordering and limit
            String orderClause = effectiveTokenSort.equals("random")
                ? "ORDER BY RANDOM()"
                : "ORDER BY total_quantity DESC";

            String tokenQuery = String.format("""
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
                %s
                LIMIT %d
                """, orderClause, effectiveMaxTokens);

            nativeTokens = jdbcTemplate.query(tokenQuery, params,
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
            nftCount = (int) nativeTokens.stream()
                .filter(token -> token.quantity().equals(BigInteger.ONE))
                .count();

            // Set message if tokens were limited
            if (totalTokenCount > effectiveMaxTokens) {
                message = String.format("⚠️ Address holds %d unique tokens. Showing %d tokens sorted by %s. " +
                                      "Increase maxTokens parameter (max: 100) for more, or use skipTokens=true for ADA-only analysis.",
                                      totalTokenCount, effectiveMaxTokens, effectiveTokenSort);
            }

            log.debug("Fetched {} tokens (total: {}, maxTokens: {}), NFTs: {}",
                     nativeTokens.size(), totalTokenCount, effectiveMaxTokens, nftCount);
        }

        return new AddressPortfolio(
            address,
            totalLovelace,
            totalAda,
            nativeTokens,
            nftCount,
            utxoCount,
            totalTokenCount,
            message
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
          description = "Get NFTs owned by an address, grouped by collection (policy ID). " +
                        "NFTs are tokens with quantity = 1. " +
                        "⚠️ CONTEXT MANAGEMENT: Returns 5 collections × 5 NFTs = 25 NFTs by default. " +
                        "RECOMMENDED WORKFLOW: " +
                        "(1) First call: maxCollections=5, maxNFTsPerCollection=3 (15 NFTs overview). " +
                        "(2) More detail: maxCollections=10, maxNFTsPerCollection=5 (50 NFTs). " +
                        "(3) Comprehensive: maxCollections=20, maxNFTsPerCollection=10 (200 NFTs max). " +
                        "Shows totals so user knows what's hidden. " +
                        "Use collectionSort='random' for variety. " +
                        "Only includes unspent UTXOs (current holdings). " +
                        "Essential for NFT galleries, collection tracking, and portfolio visualization.")
    public AddressNFTPortfolio getAddressNFTCollection(
        @ToolParam(description = "Cardano address (addr1... or addr_test...)") String address,
        @ToolParam(description = "Max collections to return (default: 5, max: 20, min: 1). Start with 5 for overview.") Integer maxCollections,
        @ToolParam(description = "Max NFTs per collection (default: 5, max: 20, min: 1). Start with 3-5 for overview.") Integer maxNFTsPerCollection,
        @ToolParam(description = "Collection sort: 'size' (default, largest first) or 'random' (variety)") String collectionSort,
        @ToolParam(description = "NFT sort within collection: 'unit' (default, alphabetical) or 'random' (variety)") String nftSort
    ) {
        // Normalize and validate parameters
        int effectiveMaxCollections = 5; // Default
        if (maxCollections != null) {
            if (maxCollections < 1) effectiveMaxCollections = 1;
            else if (maxCollections > 20) effectiveMaxCollections = 20;
            else effectiveMaxCollections = maxCollections;
        }

        int effectiveMaxNFTsPerCollection = 5; // Default
        if (maxNFTsPerCollection != null) {
            if (maxNFTsPerCollection < 1) effectiveMaxNFTsPerCollection = 1;
            else if (maxNFTsPerCollection > 20) effectiveMaxNFTsPerCollection = 20;
            else effectiveMaxNFTsPerCollection = maxNFTsPerCollection;
        }

        String effectiveCollectionSort = (collectionSort != null && collectionSort.equalsIgnoreCase("random")) ? "random" : "size";
        String effectiveNftSort = (nftSort != null && nftSort.equalsIgnoreCase("random")) ? "random" : "unit";

        log.debug("Getting NFT collection for address: {}, maxCollections: {}, maxNFTsPerCollection: {}, collectionSort: {}, nftSort: {}",
                  address, effectiveMaxCollections, effectiveMaxNFTsPerCollection, effectiveCollectionSort, effectiveNftSort);

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);

        // Step 1: Get total counts (NFTs and collections)
        String countsQuery = """
            WITH unspent_utxos AS (
                SELECT u.amounts
                FROM address_utxo u
                LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                WHERE ti.tx_hash IS NULL
                  AND u.owner_addr = :address
                  AND u.amounts IS NOT NULL
            ),
            nft_data AS (
                SELECT
                    (elem->>'policy_id') as policy_id
                FROM unspent_utxos
                CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
                WHERE (elem->>'unit') != 'lovelace'
                  AND (elem->>'quantity')::numeric = 1
            )
            SELECT
                COUNT(*) as total_nft_count,
                COUNT(DISTINCT policy_id) as total_collection_count
            FROM nft_data
            """;

        Map<String, Object> countsResult = jdbcTemplate.queryForMap(countsQuery, params);
        int totalNFTCount = ((Number) countsResult.get("total_nft_count")).intValue();
        int totalCollectionCount = ((Number) countsResult.get("total_collection_count")).intValue();

        log.debug("Total NFTs: {}, Total Collections: {}", totalNFTCount, totalCollectionCount);

        // Step 2: Get limited NFTs with per-collection limiting
        String collectionOrderClause = effectiveCollectionSort.equals("random")
            ? "ORDER BY RANDOM()"
            : "ORDER BY collection_nft_count DESC";

        String nftOrderClause = effectiveNftSort.equals("random")
            ? "RANDOM()"
            : "unit";

        String nftQuery = String.format("""
            WITH unspent_utxos AS (
                SELECT u.amounts
                FROM address_utxo u
                LEFT JOIN tx_input ti ON u.tx_hash = ti.tx_hash AND u.output_index = ti.output_index
                WHERE ti.tx_hash IS NULL
                  AND u.owner_addr = :address
                  AND u.amounts IS NOT NULL
            ),
            all_nfts AS (
                SELECT
                    (elem->>'unit') as unit,
                    (elem->>'policy_id') as policy_id,
                    (elem->>'asset_name') as asset_name,
                    (elem->>'quantity')::numeric as quantity
                FROM unspent_utxos
                CROSS JOIN LATERAL jsonb_array_elements(amounts) AS elem
                WHERE (elem->>'unit') != 'lovelace'
                  AND (elem->>'quantity')::numeric = 1
            ),
            collection_sizes AS (
                SELECT
                    policy_id,
                    COUNT(*) as collection_nft_count
                FROM all_nfts
                GROUP BY policy_id
            ),
            top_collections AS (
                SELECT policy_id, collection_nft_count
                FROM collection_sizes
                %s
                LIMIT %d
            ),
            limited_nfts AS (
                SELECT
                    n.unit,
                    n.policy_id,
                    n.asset_name,
                    n.quantity,
                    ROW_NUMBER() OVER (PARTITION BY n.policy_id ORDER BY %s) as rn
                FROM all_nfts n
                INNER JOIN top_collections tc ON n.policy_id = tc.policy_id
            )
            SELECT unit, policy_id, asset_name, quantity
            FROM limited_nfts
            WHERE rn <= %d
            ORDER BY policy_id, rn
            """, collectionOrderClause, effectiveMaxCollections, nftOrderClause, effectiveMaxNFTsPerCollection);

        List<NFT> limitedNFTs = jdbcTemplate.query(nftQuery, params,
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
        for (NFT nft : limitedNFTs) {
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

        // Calculate what's shown
        int collectionsShown = collections.size();
        int nftsShown = limitedNFTs.size();

        // Build message if anything was limited
        String message = null;
        if (totalNFTCount > nftsShown || totalCollectionCount > collectionsShown) {
            int hiddenCollections = totalCollectionCount - collectionsShown;
            int hiddenNFTs = totalNFTCount - nftsShown;

            message = String.format(
                "📊 Portfolio Summary: You own %d NFTs across %d collections.\n" +
                "Showing %d NFTs from %d collections (%d NFTs shown).\n" +
                "Hidden: %d collections with %d NFTs not shown.\n" +
                "Increase maxCollections (max: 20) or maxNFTsPerCollection (max: 20) for more.",
                totalNFTCount, totalCollectionCount,
                effectiveMaxNFTsPerCollection, collectionsShown, nftsShown,
                hiddenCollections, hiddenNFTs
            );
        }

        log.debug("Returned {} collections with {} NFTs (total: {} collections, {} NFTs)",
                 collectionsShown, nftsShown, totalCollectionCount, totalNFTCount);

        return new AddressNFTPortfolio(
            address,
            totalNFTCount,
            totalCollectionCount,
            collections,
            message
        );
    }
}
