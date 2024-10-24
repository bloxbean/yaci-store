package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.CostModelEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.CostModelRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.EpochParamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class EpochParamStorageImpl implements EpochParamStorage {
    private final EpochParamRepository epochParamRepository;
    private final CostModelRepository costModelRepository;
    private final ProtocolParamsMapper mapper;

    @Override
    public void save(EpochParam epochParam) {
        if (epochParam == null) return;

        epochParamRepository.findById(epochParam.getEpoch())
                .ifPresent(epochParamEntity -> epochParamRepository.delete(epochParamEntity));

        EpochParamEntity entity = mapper.toEntity(epochParam);

        //Save cost model if required
        var costModels = epochParam.getParams().getCostModels();
        var costModelHash = epochParam.getParams().getCostModelsHash();
        if (costModelHash != null && costModels != null) {
            boolean costModelExists = costModelRepository.existsById(costModelHash);
            if (!costModelExists) {
                var costModelEntity = CostModelEntity.builder()
                        .costs(costModels)
                        .hash(costModelHash)
                        .slot(epochParam.getSlot())
                        .blockNumber(epochParam.getBlockNumber())
                        .blockTime(epochParam.getBlockTime())
                        .build();

                costModelRepository.save(costModelEntity);
            }
        }

        epochParamRepository.save(entity);
    }

    @Override
    public Optional<EpochParam> getProtocolParams(int epoch) {
        var epochParamOpt = epochParamRepository.findById(epoch)
                .map(mapper::toDomain);

        var costs = epochParamOpt
                .map(epochParam -> epochParam.getParams().getCostModelsHash())
                .filter(costModelHash -> costModelHash != null)
                .map(costModelHash -> costModelRepository.findById(costModelHash))
                .map(costModelEntity -> costModelEntity.get().getCosts())
                .orElse(null);

        epochParamOpt
                .ifPresent(epochParam -> epochParam.getParams().setCostModels(costs));

        return epochParamOpt;
    }

    @Override
    public Optional<EpochParam> getLatestEpochParam() {
        return epochParamRepository.findLatestEpochParam()
                .map(mapper::toDomain);
    }

    @Override
    public Integer getMaxEpoch() {
        return epochParamRepository.findMaxEpoch();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return epochParamRepository.deleteBySlotGreaterThan(slot);
    }
}
