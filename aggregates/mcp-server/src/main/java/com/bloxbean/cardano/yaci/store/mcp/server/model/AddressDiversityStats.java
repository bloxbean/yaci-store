package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Portfolio diversity metrics for an address.
 * Measures how diversified a holder's portfolio is.
 */
public record AddressDiversityStats(
    int rank,
    String address,
    int uniqueTokenCount,
    int uniquePolicyCount,
    BigDecimal adaBalance,
    String diversityLevel  // "Highly Diversified", "Moderately Diversified", "Concentrated"
) {}
