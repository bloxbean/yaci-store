package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Statistics about token holders for a specific asset.
 * Provides holder count, total supply in circulation, and sample asset details.
 */
public record TokenHolderStats(
    String policyId,
    String assetName,
    String assetUnit,
    int holderCount,
    BigDecimal totalQuantity,
    int utxoCount
) {}