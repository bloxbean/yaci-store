package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

/**
 * Raw query result for a pool's retirement status.
 * Used for {@code GET /pools/retired} and {@code GET /pools/retiring}.
 */
public record BFPoolRetireItem(
        String poolId,
        Integer retireEpoch
) {
}
