package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Result model for asset name search queries.
 * Used when searching for assets by their human-readable names (e.g., "tDRIP", "DRIP").
 *
 * @param policy Policy ID (56-character hex string)
 * @param assetName Human-readable asset name (e.g., "tDRIP", "DRIP")
 * @param unit Full unit identifier (policy + asset name hex)
 * @param fingerprint CIP-14 asset fingerprint
 * @param occurrenceCount Number of mint/burn transactions for this asset
 */
public record AssetSearchResult(
    String policy,
    String assetName,
    String unit,
    String fingerprint,
    int occurrenceCount
) {}
