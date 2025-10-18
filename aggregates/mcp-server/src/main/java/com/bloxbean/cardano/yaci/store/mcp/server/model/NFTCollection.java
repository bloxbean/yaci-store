package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Model representing a collection of NFTs grouped by policy ID.
 * All NFTs with the same policy ID belong to the same collection.
 */
public record NFTCollection(
    String policyId,            // Policy ID (hex) - identifies the collection
    int nftCount,               // Number of NFTs in this collection owned by address
    List<NFT> nfts              // List of NFTs in this collection
) {}
