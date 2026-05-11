package com.bloxbean.cardano.yaci.store.epochnonce.storage;

import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;

import java.util.Optional;

public interface EpochNonceStorageReader {
    Optional<EpochNonce> findByEpoch(int epoch);
}
