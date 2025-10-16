package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Represents a single asset holding with policy, name and quantity.
 */
public record AssetHolding(
    String assetUnit,
    String policyId,
    String assetName,
    BigDecimal quantity
) {}
