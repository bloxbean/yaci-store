package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Lightweight summary of a DApp for listing operations.
 * Contains only essential information (name, display name, category) to minimize token usage.
 *
 * Used by listing tools (dapp-list-all, dapp-list-by-category) to prevent
 * Claude Desktop token overflow when displaying many DApps.
 *
 * For full DApp details (addresses, policy IDs, etc.), use dapp-lookup tool.
 */
public record DAppSummary(
    String name,           // Lowercase identifier (e.g., "minswap", "jpg.store")
    String displayName,    // Human-readable name (e.g., "Minswap", "JPG Store")
    String category        // Category: "DEX", "NFT Marketplace", "Lending", etc.
) {}
