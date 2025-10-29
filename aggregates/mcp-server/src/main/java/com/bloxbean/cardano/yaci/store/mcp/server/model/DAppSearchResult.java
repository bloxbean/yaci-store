package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Model representing search results from DApp registry lookup.
 * Provides both exact and partial matches to help users find the right DApp.
 *
 * Example use cases:
 * - User searches "sun" → finds "sundaeswap" as partial match
 * - User searches "minswap" → finds exact match
 * - User searches "dex" → finds multiple partial matches in category or description
 */
public record DAppSearchResult(
    List<DAppInfo> exactMatches,       // DApps where name matches exactly (case-insensitive)
    List<DAppInfo> partialMatches,     // DApps where name contains search term
    String searchTerm,                 // The original search term
    String network,                    // Network context (mainnet/preprod/preview)
    int totalResults,                  // Total number of matches (exact + partial)
    String searchNote                  // Helpful note for the user (e.g., "Try searching by category")
) {
    /**
     * Create a search result with automatic calculation of total and helpful note.
     */
    public static DAppSearchResult create(
        List<DAppInfo> exactMatches,
        List<DAppInfo> partialMatches,
        String searchTerm,
        String network
    ) {
        int total = exactMatches.size() + partialMatches.size();

        String note;
        if (total == 0) {
            note = "No DApps found. Try searching by category (e.g., 'DEX', 'NFT Marketplace') or use dapp-list-all to see available DApps.";
        } else if (exactMatches.isEmpty()) {
            note = "Showing partial matches. For exact match, try the full DApp name.";
        } else {
            note = "Found exact match(es). Use dapp-reverse-lookup to identify addresses.";
        }

        return new DAppSearchResult(
            exactMatches,
            partialMatches,
            searchTerm,
            network,
            total,
            note
        );
    }
}
