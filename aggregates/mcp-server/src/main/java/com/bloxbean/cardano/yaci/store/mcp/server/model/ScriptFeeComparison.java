package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Comparative fee metrics for script analysis.
 * Used for competitive analysis of DeFi protocols.
 * Includes revenue per user calculation.
 */
public record ScriptFeeComparison(
    String scriptAddress,
    long transactionCount,
    BigDecimal totalFees,
    BigDecimal averageFee,
    int uniqueUsers,
    BigDecimal revenuePerUser
) {}
