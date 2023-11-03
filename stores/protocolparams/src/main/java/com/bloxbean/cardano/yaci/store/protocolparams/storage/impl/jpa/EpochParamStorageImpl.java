package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.protocolparams.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.CostModelEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.CostModelRepository;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.EpochParamRepository;
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
        return epochParamRepository.findById(epoch)
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
