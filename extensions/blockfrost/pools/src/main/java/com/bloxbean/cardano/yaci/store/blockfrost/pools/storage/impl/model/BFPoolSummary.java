package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

import java.math.BigInteger;
import java.util.List;

/**
 * Raw query result for a pool's latest registration details.
 * Used for {@code GET /pools/{pool_id}}.
 */
public record BFPoolSummary(
        String poolId,
        String vrfKey,
        BigInteger pledge,
        BigInteger cost,
        BigInteger marginNumerator,
        BigInteger marginDenominator,
        String rewardAccount,
        List<String> owners,
        int blocksMinted,
        int blocksEpoch,
        List<String> registration,
        List<String> retirement
) {
}
