package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Wealth distribution and concentration metrics for a token.
 * Includes Gini coefficient and top percentile analysis.
 */
public record HolderConcentrationStats(
    String assetUnit,
    String policyId,
    String assetName,
    int totalHolders,
    BigDecimal totalSupply,

    // Top percentile concentration
    BigDecimal top1PercentHoldings,
    BigDecimal top5PercentHoldings,
    BigDecimal top10PercentHoldings,
    BigDecimal top25PercentHoldings,

    // Top N holders concentration
    BigDecimal top10HoldersPercent,
    BigDecimal top100HoldersPercent,

    // Distribution metrics
    BigDecimal giniCoefficient,
    BigDecimal medianHolding,
    BigDecimal meanHolding,

    // Concentration assessment
    String concentrationLevel  // "Highly Concentrated", "Moderately Concentrated", "Well Distributed"
) {}
