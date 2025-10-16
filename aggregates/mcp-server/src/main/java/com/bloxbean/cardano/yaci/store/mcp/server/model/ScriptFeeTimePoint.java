package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Single point in a script fee timeline.
 * Represents fee collection for a specific epoch.
 * Used for trend analysis and growth tracking.
 */
public record ScriptFeeTimePoint(
    int epoch,
    long transactionCount,
    BigDecimal totalFees,
    BigDecimal averageFee,
    int uniqueUsers
) {}
