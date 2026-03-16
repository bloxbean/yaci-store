package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model;

/**
 * Raw storage model for a script row.
 * The {@code content} column stores JSON like {@code {"content":"<hex>"}}.
 * For native scripts the inner hex decodes to JSON; for Plutus scripts it is CBOR hex.
 */
public record BFScript(
        String scriptHash,
        String scriptType,   // e.g. "NATIVE_SCRIPT", "PLUTUS_V1", "PLUTUS_V2", "PLUTUS_V3"
        String content       // raw JSON column value
) {}
