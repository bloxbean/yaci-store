package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Model representing comprehensive statistics for a stake pool.
 * Combines block production, delegation, and reward data.
 */
public record PoolCompleteStats(
    String poolId,         // Pool ID in hex format
    String poolIdBech32,   // Pool ID in bech32 format (pool1...)
    Integer epoch,
    // Block production metrics
    int blocksProduced,
    long totalTransactions,
    BigDecimal totalFees,
    // Delegation metrics
    int delegatorCount,
    BigInteger totalStake,
    // Reward metrics
    BigDecimal totalRewards,
    int rewardRecipients
) {}
