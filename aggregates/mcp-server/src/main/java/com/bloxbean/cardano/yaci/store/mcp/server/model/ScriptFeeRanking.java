package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Ranking entry for scripts by fee collection.
 * Used to identify top DeFi protocols and market leaders.
 */
public record ScriptFeeRanking(
    String scriptAddress,
    long transactionCount,
    BigDecimal totalFees,
    BigDecimal averageFee,
    int uniqueUsers,
    int firstEpoch,
    int lastEpoch
) {}
