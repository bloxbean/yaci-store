package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

/**
 * Raw query result for pool on-chain metadata from latest registration.
 * Used for {@code GET /pools/{pool_id}/metadata}.
 */
public record BFPoolMetadata(
        String poolId,
        String metadataUrl,
        String metadataHash
) {
}
