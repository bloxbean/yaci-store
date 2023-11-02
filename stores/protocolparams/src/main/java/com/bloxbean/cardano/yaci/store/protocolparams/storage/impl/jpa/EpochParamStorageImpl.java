package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.protocolparams.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.EpochParamRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class EpochParamStorageImpl implements EpochParamStorage {
    private final EpochParamRepository repository;
    private final ProtocolParamsMapper mapper;

    @Override
    public void save(EpochParam epochParam) {
        if (epochParam == null) return;

        repository.findById(epochParam.getEpoch())
                .ifPresent(epochParamEntity -> repository.delete(epochParamEntity));

        EpochParamEntity entity = mapper.toEntity(epochParam);
        repository.save(entity);
    }

    @Override
    public Optional<EpochParam> getProtocolParams(int epoch) {
        return repository.findById(epoch)
                .map(mapper::toDomain);
    }

    @Override
    public Integer getMaxEpoch() {
        return repository.findMaxEpoch();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return repository.deleteBySlotGreaterThan(slot);
    }
}
