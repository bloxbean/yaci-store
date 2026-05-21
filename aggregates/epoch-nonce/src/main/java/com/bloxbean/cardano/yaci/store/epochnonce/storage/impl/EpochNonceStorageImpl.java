package com.bloxbean.cardano.yaci.store.epochnonce.storage.impl;

import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.mapper.EpochNonceMapper;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.repository.EpochNonceRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
public class EpochNonceStorageImpl implements EpochNonceStorage {

    private final EpochNonceRepository epochNonceRepository;
    private final EpochNonceMapper epochNonceMapper;

    @Override
    public void save(@NonNull EpochNonce epochNonce) {
        epochNonceRepository.save(epochNonceMapper.toEpochNonceEntity(epochNonce));
    }

    @Override
    public Optional<EpochNonce> findByEpoch(int epoch) {
        return epochNonceRepository.findByEpoch(epoch)
                .map(epochNonceMapper::toEpochNonce);
    }

    @Override
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        return epochNonceRepository.deleteBySlotGreaterThan(slot);
    }
}
