package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Model representing all NFTs owned by an address, grouped by collection (policy ID).
 * Makes it easy to see which NFT collections an address holds.
 * Supports limiting for context management with clear messaging about what's shown vs hidden.
 */
public record AddressNFTPortfolio(
    String address,                     // Address (addr1... or addr_test...)
    int totalNFTCount,                  // Total number of NFTs across ALL collections (before limiting)
    int totalCollectionCount,           // Total number of unique collections (before limiting)
    List<NFTCollection> collections,    // NFTs grouped by collection (after limiting - use .size() for shown count)
    String message                      // Information for LLM about limiting (null if nothing hidden)
) {}
