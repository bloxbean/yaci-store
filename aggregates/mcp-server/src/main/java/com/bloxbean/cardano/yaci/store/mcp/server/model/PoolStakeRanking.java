package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing a ranked stake pool by total stake.
 * Used for pool comparison and discovery based on stake size.
 */
public record PoolStakeRanking(
    String poolId,                 // Pool ID in hex format
    String poolIdBech32,           // Pool ID in bech32 format (pool1...)
    int delegatorCount,            // Number of unique delegators
    BigInteger totalStake,         // Total stake delegated to pool (lovelace)
    double stakePercentage,        // Stake as % of total network stake
    double saturationPct,          // Saturation percentage
    String saturationLevel,        // HEALTHY, APPROACHING_SATURATION, OVERSATURATED
    int rank,                      // Position in ranking (1-based)
    Integer epoch                  // Epoch for this ranking (null for all-time)
) {}
