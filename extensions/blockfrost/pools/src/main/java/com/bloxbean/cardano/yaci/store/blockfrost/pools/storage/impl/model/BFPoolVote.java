package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

/**
 * Raw query result for a pool governance vote.
 * Used for {@code GET /pools/{pool_id}/votes}.
 */
public record BFPoolVote(
        String txHash,
        Integer certIndex,
        String vote
) {
}
