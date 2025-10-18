package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Model representing all NFTs owned by an address, grouped by collection (policy ID).
 * Makes it easy to see which NFT collections an address holds.
 */
public record AddressNFTPortfolio(
    String address,                     // Address (addr1... or addr_test...)
    int totalNFTCount,                  // Total number of NFTs across all collections
    int collectionCount,                // Number of unique collections (policy IDs)
    List<NFTCollection> collections     // NFTs grouped by collection/policy ID
) {}
