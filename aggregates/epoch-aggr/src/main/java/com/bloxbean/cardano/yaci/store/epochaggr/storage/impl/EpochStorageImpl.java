package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorage;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.JpaEpochEntity;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.repository.EpochRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class EpochStorageImpl implements EpochStorage {

    private final EpochRepository epochRepository;
    private final EpochMapper epochMapper;

    @Override
    public void save(Epoch epoch) {
        JpaEpochEntity updatedEpochEntity = epochRepository.findById(epoch.getNumber())
                .map(epochEntity -> {
                    epochMapper.updateEntity(epoch, epochEntity);
                    return epochEntity;
                }).orElse(epochMapper.toEpochEntity(epoch));

        epochRepository.save(updatedEpochEntity);
    }

    @Override
    public Optional<Epoch> findByNumber(int number) {
        return epochRepository.findById((long)number)
                .map(epochEntity -> epochMapper.toEpoch(epochEntity));
    }

}
