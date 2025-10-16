package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing Plutus datum with both CBOR and JSON representations.
 * Used for smart contract data inspection and analysis.
 */
public record DatumDetails(
    String hash,           // Datum hash (blake2b-256)
    String datumCbor,      // CBOR hex representation
    String datumJson,      // JSON representation (converted from PlutusData)
    String createdAtTx     // Transaction hash where datum was first created
) {}
