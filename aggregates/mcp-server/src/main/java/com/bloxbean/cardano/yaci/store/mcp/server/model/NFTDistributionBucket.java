package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Represents a bucket in the NFT distribution histogram.
 * Shows how many addresses hold a specific number of NFTs from a collection.
 */
public record NFTDistributionBucket(
    int nftCount,           // Number of NFTs held (bucket label)
    int addressCount,       // How many addresses hold this many NFTs
    BigDecimal totalQuantity, // Total quantity in this bucket
    BigDecimal percentOfHolders  // Percentage of total holders in this bucket
) {}
