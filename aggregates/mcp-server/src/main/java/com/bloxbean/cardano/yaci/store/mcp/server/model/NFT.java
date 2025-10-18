package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing a single NFT (Non-Fungible Token).
 * NFTs are tokens with quantity = 1.
 */
public record NFT(
    String unit,                // Full asset unit (policyId + assetName hex)
    String policyId,            // Policy ID (hex) - identifies the collection
    String assetName,           // Asset name (UTF-8 decoded if possible)
    String assetNameHex,        // Asset name in hex format
    BigInteger quantity         // Always 1 for NFTs
) {
    /**
     * Create NFT from TokenBalance.
     */
    public static NFT fromTokenBalance(TokenBalance token) {
        return new NFT(
            token.unit(),
            token.policyId(),
            token.assetName(),
            token.assetNameHex(),
            token.quantity()
        );
    }
}
