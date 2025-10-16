package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Fee analytics for a specific script address.
 * Tracks total fees paid when spending UTXOs from a script address.
 * Essential for measuring DeFi protocol revenue and usage.
 */
public record ScriptFeeAnalytics(
    String scriptAddress,
    long transactionCount,
    BigDecimal totalFees,
    BigDecimal averageFee,
    BigDecimal minFee,
    BigDecimal maxFee,
    int uniqueSpenders
) {}
