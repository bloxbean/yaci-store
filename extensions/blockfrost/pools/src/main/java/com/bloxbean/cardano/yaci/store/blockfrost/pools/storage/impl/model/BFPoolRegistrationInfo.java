package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

import java.math.BigInteger;

/**
 * Raw query result for a pool in the extended list.
 * Used for {@code GET /pools/extended}.
 */
public record BFPoolRegistrationInfo(
        String poolId,
        String vrfKey,
        BigInteger pledge,
        BigInteger cost,
        BigInteger marginNumerator,
        BigInteger marginDenominator,
        String metadataUrl,
        String metadataHash,
        int blocksMinted
) {
}
