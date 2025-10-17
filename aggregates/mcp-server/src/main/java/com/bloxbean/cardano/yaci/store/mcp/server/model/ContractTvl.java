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
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContractTvl(
    String scriptHash,
    String scriptType,
    Long utxoCount,                      // Number of UTXOs locked at script address
    BigInteger totalLovelace,            // Total ADA locked (in lovelace)
    BigDecimal totalAda,                 // Total ADA locked (human-readable)
    Map<String, BigInteger> tokenBalances // Token unit -> quantity map
) {
    /**
     * Factory method to create ContractTvl with automatic lovelace to ADA conversion.
     */
    public static ContractTvl create(
            String scriptHash,
            String scriptType,
            Long utxoCount,
            BigInteger totalLovelace,
            Map<String, BigInteger> tokenBalances) {

        return new ContractTvl(
                scriptHash,
                scriptType,
                utxoCount,
                totalLovelace,
                lovelaceToAda(totalLovelace),
                tokenBalances
        );
    }
}
