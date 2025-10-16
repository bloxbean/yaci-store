package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete portfolio for a stake address including ADA and all assets.
 * Aggregates holdings across all addresses delegating to the stake address.
 */
public record StakeAddressPortfolio(
    String stakeAddress,
    long utxoCount,
    BigDecimal totalLovelace,
    List<AssetHolding> assets
) {}
