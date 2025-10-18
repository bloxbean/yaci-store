package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Model representing pool cost and fee impact analysis.
 * Used for analyzing how pool fees affect delegator rewards.
 */
public record PoolCostAnalysis(
    String poolId,                             // Pool ID in hex format
    String poolIdBech32,                       // Pool ID in bech32 format (pool1...)
    BigInteger fixedCost,                      // Pool fixed cost per epoch (lovelace)
    BigDecimal margin,                         // Pool margin (0-1, e.g., 0.01 = 1%)
    BigInteger pledge,                         // Pool pledge (lovelace)
    BigDecimal avgDelegators,                  // Average number of delegators
    BigInteger avgPoolStake,                   // Average pool stake over period (lovelace)
    BigDecimal totalRewards,                   // Total rewards distributed (lovelace)
    int epochCount,                            // Number of epochs analyzed
    BigInteger fixedCostPerDelegatorPerEpoch,  // Fixed cost impact per delegator (lovelace, whole units)
    BigInteger estRewardsPer10kAdaPerEpoch,    // Estimated rewards per 10k ADA per epoch (lovelace, whole units)
    BigInteger estRewardsAfterMargin,          // Estimated rewards after margin per 10k ADA (lovelace, whole units)
    BigInteger netRewardsPer10kAdaPerEpoch,    // Net rewards after fees & margin (lovelace, whole units)
    BigDecimal annualizedRoaPct,               // Annualized ROA percentage
    Integer startEpoch,                        // Start of analysis period
    Integer endEpoch                           // End of analysis period
) {}
