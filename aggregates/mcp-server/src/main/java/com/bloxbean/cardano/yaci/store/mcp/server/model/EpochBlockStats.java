package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Block production statistics for a specific epoch.
 * Provides network-wide block production metrics.
 */
public record EpochBlockStats(
    int epoch,
    int blockCount,
    long totalTransactions,
    BigDecimal totalFees,
    double avgTxsPerBlock,
    double avgBlockSize,
    int uniquePoolCount
) {}
