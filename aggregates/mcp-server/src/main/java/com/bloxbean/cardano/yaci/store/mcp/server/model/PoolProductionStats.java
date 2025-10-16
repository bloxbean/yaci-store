package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Block production statistics for a stake pool.
 * Used for pool performance analysis and monitoring.
 */
public record PoolProductionStats(
    int epoch,
    int blocksProduced,
    long totalTransactions,
    BigDecimal totalFees,
    double avgTxsPerBlock
) {}
