package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Result of converting a bech32 governance action ID to its components.
 * Contains the transaction hash and governance action index extracted from the bech32 ID.
 */
public record GovActionIdResult(
    String govActionTxHash,
    Integer govActionIndex,
    String originalBech32
) {
    public static GovActionIdResult create(String txHash, Integer index, String bech32) {
        return new GovActionIdResult(txHash, index, bech32);
    }
}