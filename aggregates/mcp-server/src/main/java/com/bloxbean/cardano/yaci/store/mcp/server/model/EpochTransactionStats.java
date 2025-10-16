package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Transaction statistics for a specific epoch.
 * Provides comprehensive metrics about network transaction activity.
 */
public record EpochTransactionStats(
    int epoch,
    long txCount,
    BigDecimal totalFees,
    BigDecimal avgFee,
    int blockCount,
    long validTxCount,
    long invalidTxCount
) {}
