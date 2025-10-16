package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Fee distribution statistics for analyzing transaction fee patterns.
 * Provides percentile-based breakdown of fee distribution.
 */
public record FeeDistributionStats(
    int epoch,
    long txCount,
    BigDecimal totalFees,
    BigDecimal minFee,
    BigDecimal maxFee,
    BigDecimal avgFee,
    BigDecimal medianFee,
    BigDecimal p25Fee,
    BigDecimal p75Fee,
    BigDecimal p90Fee,
    BigDecimal p95Fee,
    BigDecimal p99Fee
) {}
