package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing pool saturation information.
 * Used for analyzing pool stake levels and saturation status.
 */
public record PoolSaturationInfo(
    String poolId,                     // Pool ID in hex format
    String poolIdBech32,               // Pool ID in bech32 format (pool1...)
    Integer epoch,                     // Epoch for this saturation check
    BigInteger poolStake,              // Total stake delegated to pool (lovelace)
    int delegatorCount,                // Number of unique delegators
    BigInteger totalNetworkStake,      // Total circulating supply (lovelace) - used for saturation calculation
    int nOpt,                          // Optimal number of pools (from protocol params)
    BigInteger optimalStakeThreshold,  // Optimal stake per pool: circulation / k (lovelace)
    double saturationPct,              // Saturation percentage (0-100+)
    String saturationLevel             // HEALTHY, APPROACHING_SATURATION, OVERSATURATED
) {}
