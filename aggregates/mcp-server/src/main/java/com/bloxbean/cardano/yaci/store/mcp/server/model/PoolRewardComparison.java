package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Model representing reward comparison statistics for a pool.
 * Used for comparing reward performance across multiple pools.
 */
public record PoolRewardComparison(
    String poolId,                      // Pool ID in hex format
    String poolIdBech32,                // Pool ID in bech32 format (pool1...)
    BigDecimal totalRewards,            // Total rewards distributed (lovelace)
    int rewardEventCount,               // Number of reward events
    int uniqueRecipients,               // Unique addresses receiving rewards
    BigInteger avgRewardPerRecipient,   // Average reward per recipient (lovelace, whole units)
    BigInteger avgPoolStake,            // Average pool stake over period (lovelace)
    BigInteger rewardsPer10kAda,        // Rewards per 10k ADA staked (lovelace, whole units)
    Integer startEpoch,                 // Start of epoch range
    Integer endEpoch                    // End of epoch range
) {}
