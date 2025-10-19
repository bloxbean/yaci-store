package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Model representing the distribution of token holders.
 * Includes whale analysis and concentration metrics.
 */
public record TokenHolderDistribution(
    String assetUnit,               // Asset unit (policyId + assetName hex)
    String policyId,                // Policy ID
    String assetName,               // Asset name (hex)
    int totalHolders,               // Total number of unique holders
    BigInteger totalSupply,         // Total supply (whole units)
    List<TopHolder> topHolders,     // Top N holders (default top 10)
    BigDecimal top10Concentration,  // % of supply held by top 10 holders
    BigDecimal giniCoefficient      // Gini coefficient (0 = perfect equality, 1 = perfect inequality)
) {
    /**
     * Create TokenHolderDistribution with calculated metrics.
     */
    public static TokenHolderDistribution create(
        String assetUnit,
        String policyId,
        String assetName,
        int totalHolders,
        BigInteger totalSupply,
        List<TopHolder> topHolders
    ) {
        // Calculate top 10 concentration (sum of top 10 holders' percentages)
        BigDecimal top10Concentration = topHolders.stream()
            .limit(10)
            .map(TopHolder::percentageOfSupply)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Gini coefficient
        BigDecimal giniCoefficient = calculateGiniCoefficient(topHolders, totalSupply);

        return new TokenHolderDistribution(
            assetUnit,
            policyId,
            assetName,
            totalHolders,
            totalSupply,
            topHolders,
            top10Concentration,
            giniCoefficient
        );
    }

    /**
     * Calculate Gini coefficient for wealth distribution.
     * Simplified calculation using top holders as proxy.
     * 0 = perfect equality, 1 = perfect inequality
     */
    private static BigDecimal calculateGiniCoefficient(List<TopHolder> holders, BigInteger totalSupply) {
        if (holders.isEmpty() || totalSupply.equals(BigInteger.ZERO)) {
            return BigDecimal.ZERO;
        }

        // Simplified Gini using top holders
        // Formula: (sum of (2*i - n - 1) * x_i) / (n * sum of x_i)
        // where i is rank, n is number of holders, x_i is quantity

        int n = holders.size();
        BigDecimal sumWeighted = BigDecimal.ZERO;

        for (int i = 0; i < holders.size(); i++) {
            TopHolder holder = holders.get(i);
            int rank = i + 1; // 1-indexed rank
            BigDecimal weight = new BigDecimal(2 * rank - n - 1);
            BigDecimal quantity = new BigDecimal(holder.quantity());
            sumWeighted = sumWeighted.add(weight.multiply(quantity));
        }

        BigDecimal totalSupplyDecimal = new BigDecimal(totalSupply);
        BigDecimal denominator = new BigDecimal(n).multiply(totalSupplyDecimal);

        if (denominator.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        // Absolute value and normalize to 0-1 range
        BigDecimal gini = sumWeighted.divide(denominator, 6, BigDecimal.ROUND_HALF_UP).abs();

        // Clamp to [0, 1]
        if (gini.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        if (gini.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return gini;
    }
}
