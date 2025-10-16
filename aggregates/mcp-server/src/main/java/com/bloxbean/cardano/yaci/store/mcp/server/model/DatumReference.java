package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing a datum reference in a transaction.
 * Helper model for datum-transaction relationships.
 */
public record DatumReference(
    String datumHash,      // Hash of the datum
    String txHash,         // Transaction using this datum
    String scriptHash,     // Script associated with this datum
    String purpose         // Purpose: "spend", "mint", "cert", "reward"
) {}
