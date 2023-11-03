package com.bloxbean.cardano.yaci.store.epoch.service;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.store.epoch.mapper.DomainMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.api.EpochParamStorage;
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
                .map(epochParam -> epochParam.getParams())
                .map(protocolParams -> mapper.toCCLProtocolParams(protocolParams));
    }

    public Optional<ProtocolParams> getProtocolParams(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(epochParam -> epochParam.getParams())
                .map(protocolParams -> mapper.toCCLProtocolParams(protocolParams));
    }

    public EpochContent getLatestEpoch() {
        var latestEpoch = epochParamStorage.getMaxEpoch();
        return EpochContent.builder()
                .epoch(latestEpoch)
                .build();
    }
}
