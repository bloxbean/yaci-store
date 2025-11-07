package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete portfolio for a stake address including ADA and all assets.
 * Aggregates holdings across all addresses delegating to the stake address.
 * Returns top 50 assets by quantity to prevent context overflow.
 */
public record StakeAddressPortfolio(
    String stakeAddress,
    long utxoCount,
    BigDecimal totalLovelace,
    int totalAssetCount,
    List<AssetHolding> assets,
    String message
) {}
