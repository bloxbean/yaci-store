package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Summary of all assets held by address(es).
 * Returns top 50 assets by quantity to prevent context overflow.
 * Includes message indicating if there are more assets beyond the top 50.
 */
public record MultiAssetSummary(
    int totalAssetCount,
    List<AssetHolding> assets,
    String message
) {}
