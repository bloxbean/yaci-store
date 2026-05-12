package com.bloxbean.cardano.yaci.store.epochnonce.storage;

import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;

import java.util.Optional;

public interface EpochNonceStorage {
    void save(EpochNonce epochNonce);

    Optional<EpochNonce> findByEpoch(int epoch);

    int deleteBySlotGreaterThan(long slot);
}
