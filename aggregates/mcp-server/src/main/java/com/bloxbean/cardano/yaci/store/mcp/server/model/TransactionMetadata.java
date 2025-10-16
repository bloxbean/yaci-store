package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing transaction metadata from the blockchain.
 * Contains both CBOR and JSON representations.
 */
public record TransactionMetadata(
    String id,             // UUID identifier
    Long slot,             // Slot number when metadata was created
    String txHash,         // Transaction hash
    String label,          // Metadata label (e.g., "721" for NFTs, "20" for FTs)
    String bodyJson,       // JSON representation (from body column)
    String cbor,           // CBOR hex representation
    Long block,            // Block number
    Long blockTime         // Block timestamp (Unix epoch)
) {}
