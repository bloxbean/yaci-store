package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.lovelaceToAda;

/**
 * Total Value Locked (TVL) estimation for a smart contract.
 * Includes both ADA and native tokens locked in UTXOs at script addresses.
 *
 * Note: addressCount field represents different things depending on mode:
 * - Optimized mode (balance tables): Number of unique addresses with balances
 * - Fallback mode (UTXO): Number of unspent UTXOs
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContractTvl(
    String scriptHash,
    String scriptType,
    long addressCount,                   // Number of unique addresses (or UTXOs in fallback mode)
    BigInteger totalLovelace,            // Total ADA locked (in lovelace)
    BigDecimal totalAda,                 // Total ADA locked (human-readable)
    Map<String, BigInteger> tokenBalances, // Token unit -> quantity map
    int totalTokenCount,                 // Total number of unique tokens (before limiting)
    boolean tokensLimited,               // True if tokenBalances was truncated
    String message                       // Optional message (e.g., truncation notice)
) {
    /**
     * Factory method to create ContractTvl with automatic lovelace to ADA conversion.
     */
    public static ContractTvl create(
            String scriptHash,
            String scriptType,
            long addressCount,
            BigInteger totalLovelace,
            Map<String, BigInteger> tokenBalances,
            int totalTokenCount,
            boolean tokensLimited) {

        String message = null;
        if (tokensLimited && totalTokenCount > tokenBalances.size()) {
            message = String.format("Note: Contract has %d unique tokens, but only top %d by quantity are returned. " +
                    "Set includeTokens=true and increase maxTokens parameter to retrieve more.",
                    totalTokenCount, tokenBalances.size());
        }

        return new ContractTvl(
                scriptHash,
                scriptType,
                addressCount,
                totalLovelace,
                lovelaceToAda(totalLovelace),
                tokenBalances,
                totalTokenCount,
                tokensLimited,
                message
        );
    }
}
