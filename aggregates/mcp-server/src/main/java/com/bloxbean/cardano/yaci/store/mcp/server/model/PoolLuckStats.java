package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing pool luck statistics for block production.
 * Compares actual blocks produced vs expected blocks based on stake distribution.
 */
public record PoolLuckStats(
    String poolId,                 // Pool ID in hex format
    String poolIdBech32,           // Pool ID in bech32 format (pool1...)
    int epoch,                     // Epoch number
    BigInteger poolStake,          // Pool's active stake (lovelace)
    BigInteger totalNetworkStake,  // Total network active stake (lovelace)
    double stakePercentage,        // Pool's stake as % of network
    int totalEpochBlocks,          // Total blocks produced in epoch by all pools
    double expectedBlocks,         // Expected blocks based on stake %
    int actualBlocks,              // Actual blocks produced
    double luckPercentage          // Luck % (100 = expected, >100 = lucky, <100 = unlucky)
) {}
