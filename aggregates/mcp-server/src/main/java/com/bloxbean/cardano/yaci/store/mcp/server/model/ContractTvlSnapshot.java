package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Historical TVL snapshot for a smart contract at a specific epoch.
 * Used for tracking TVL evolution over time.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContractTvlSnapshot(
    int epoch,
    String scriptHash,
    long addressCount,
    BigInteger totalLovelace,
    Map<String, BigInteger> tokenBalances
) {
    /**
     * Get human-readable ADA amount.
     */
    public BigDecimal totalAda() {
        return new BigDecimal(totalLovelace)
            .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
    }
}
