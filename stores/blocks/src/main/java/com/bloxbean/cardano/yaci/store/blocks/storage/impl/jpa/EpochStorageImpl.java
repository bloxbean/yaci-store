package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.EpochStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.EpochEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.EpochRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class EpochStorageImpl implements EpochStorage {

    private final EpochRepository epochRepository;
    private final EpochMapper epochMapper;

    @Override
    public void save(Epoch epoch) {
        EpochEntity updatedEpochEntity = epochRepository.findById(epoch.getNumber())
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
