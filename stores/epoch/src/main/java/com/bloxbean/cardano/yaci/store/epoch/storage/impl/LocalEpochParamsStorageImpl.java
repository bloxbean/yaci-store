package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.LocalEpochParamsEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.LocalEpochParamsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class LocalEpochParamsStorageImpl implements LocalEpochParamsStorage {
    private final LocalEpochParamsRepository localEpochParamsRepository;

    @Override
    public void save(EpochParam epochParam) {
        if (epochParam == null) return;

        LocalEpochParamsEntity entity = LocalEpochParamsEntity.builder()
                .epoch(epochParam.getEpoch())
                .protocolParams(epochParam.getParams())
                .build();

        localEpochParamsRepository.save(entity);
    }

    @Override
    public Optional<EpochParam> getEpochParam(int epoch) {
        return localEpochParamsRepository.findById(epoch)
                .map(entity -> EpochParam.builder()
                        .epoch(entity.getEpoch())
                        .params(entity.getProtocolParams())
                        .build());
    }

    @Override
    public Optional<EpochParam> getLatestEpochParam() {
        return localEpochParamsRepository.findLatest()
                .map(entity -> EpochParam.builder()
                        .epoch(entity.getEpoch())
                        .params(entity.getProtocolParams())
                        .build());
    }

    @Override
    public Optional<Integer> getMaxEpoch() {
        return localEpochParamsRepository.findMaxEpoch();
    }
}
