package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Model representing aggregated mint/burn information for a token from the assets table.
 * Tracks historical minting activity including tokens that may have been fully burned.
 */
public record AssetMintInfo(
    String policy,
    String assetName,
    String unit,
    String fingerprint,
    BigDecimal netQuantity,        // Total mints - burns
    int mintBurnCount,              // Number of mint/burn transactions
    Long firstMintSlot,             // Slot of first mint transaction
    Long lastActivitySlot,          // Slot of most recent mint/burn
    Long firstMintTime,             // Timestamp of first mint
    Long lastActivityTime           // Timestamp of most recent activity
) {}
