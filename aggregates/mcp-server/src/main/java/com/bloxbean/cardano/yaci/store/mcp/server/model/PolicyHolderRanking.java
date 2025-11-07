package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Represents a top holder of tokens from a specific policy (aggregated across all assets).
 * Used for NFT collection whale analysis.
 */
public record PolicyHolderRanking(
    int rank,
    String address,
    String policyId,
    int uniqueAssetCount,
    BigDecimal totalQuantity
) {}
