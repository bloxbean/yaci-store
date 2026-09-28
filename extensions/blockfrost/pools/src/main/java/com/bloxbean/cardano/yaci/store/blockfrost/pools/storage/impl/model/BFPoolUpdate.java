package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

/**
 * Raw query result for a pool certificate (registration or retirement).
 * Used for {@code GET /pools/{pool_id}/updates}.
 */
public record BFPoolUpdate(
        String txHash,
        Integer certIndex,
        String action
) {
}
