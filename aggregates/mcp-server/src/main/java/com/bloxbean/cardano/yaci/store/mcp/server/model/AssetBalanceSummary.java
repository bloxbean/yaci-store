package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Balance summary for a specific asset across one or more addresses.
 */
public record AssetBalanceSummary(
    String assetUnit,
    int holderCount,
    BigDecimal totalQuantity
) {}
