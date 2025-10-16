package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing a ranked stake pool by delegator count.
 * Used for pool comparison and discovery.
 */
public record PoolRanking(
    String poolId,         // Pool ID in hex format
    String poolIdBech32,   // Pool ID in bech32 format (pool1...)
    int delegatorCount,
    int rank,              // Position in ranking (1-based)
    Integer epoch          // Epoch for this ranking (null for all-time)
) {}
