package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model;

/**
 * Raw storage model for a datum row.
 * The {@code datum} column stores CBOR hex of the PlutusData.
 */
public record BFDatum(
        String hash,
        String cborHex   // value from datum.datum column (CBOR hex)
) {}
