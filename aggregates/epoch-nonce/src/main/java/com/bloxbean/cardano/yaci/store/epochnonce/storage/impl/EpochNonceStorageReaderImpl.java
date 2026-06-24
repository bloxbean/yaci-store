package com.bloxbean.cardano.yaci.store.epochnonce.storage.impl;

import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorageReader;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.mapper.EpochNonceMapper;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.repository.EpochNonceRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class EpochNonceStorageReaderImpl implements EpochNonceStorageReader {

    private final EpochNonceRepository epochNonceRepository;
    private final EpochNonceMapper epochNonceMapper;

    @Override
    public Optional<EpochNonce> findByEpoch(int epoch) {
        return epochNonceRepository.findByEpoch(epoch)
                .map(epochNonceMapper::toEpochNonce);
    }
}
