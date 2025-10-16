package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Model representing reward distribution statistics for a stake pool.
 * Aggregates rewards distributed to delegators by the pool.
 */
public record PoolRewardStats(
    String poolId,                     // Pool ID in hex format
    String poolIdBech32,               // Pool ID in bech32 format (pool1...)
    BigDecimal totalRewards,           // Total rewards in lovelace
    int rewardEventCount,              // Number of reward distribution events
    int uniqueRecipients,              // Unique addresses receiving rewards
    BigDecimal avgRewardPerRecipient,  // Average reward per unique recipient
    Integer startEpoch,                // Start of epoch range (null for all-time)
    Integer endEpoch                   // End of epoch range (null for all-time)
) {}
