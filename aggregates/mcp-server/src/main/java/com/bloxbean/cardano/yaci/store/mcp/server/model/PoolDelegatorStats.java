package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing delegation statistics for a stake pool from epoch stake snapshots.
 * Uses official protocol snapshots taken at epoch boundaries.
 */
public record PoolDelegatorStats(
    String poolId,             // Pool ID in hex format
    String poolIdBech32,       // Pool ID in bech32 format (pool1...)
    int delegatorCount,        // Unique addresses delegating to pool
    BigInteger totalStake,     // Total stake amount in lovelace
    Integer activeEpoch        // Epoch when stake is/was active (null for latest)
) {}
