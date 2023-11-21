package com.bloxbean.cardano.yaci.store.api.epoch.service;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochParamService {

    private final EpochParamStorage epochParamStorage;
    private final DomainMapper mapper = DomainMapper.INSTANCE;

    public Optional<ProtocolParams> getLatestProtocolParams() {
        int epoch = epochParamStorage.getMaxEpoch();
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toCCLProtocolParams);
    }

    public Optional<ProtocolParams> getProtocolParams(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toCCLProtocolParams);
    }

    public EpochContent getLatestEpoch() {
        var latestEpoch = epochParamStorage.getMaxEpoch();
        return EpochContent.builder()
                .epoch(latestEpoch)
                .build();
    }
}
