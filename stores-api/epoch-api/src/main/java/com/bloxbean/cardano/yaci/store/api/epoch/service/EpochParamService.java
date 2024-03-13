package com.bloxbean.cardano.yaci.store.api.epoch.service;

import com.bloxbean.cardano.yaci.store.api.epoch.dto.EpochNo;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
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

    public Optional<ProtocolParamsDto> getLatestProtocolParams() {
        int epoch = epochParamStorage.getMaxEpoch();
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toProtocolParamsDto);
    }

    public Optional<ProtocolParamsDto> getProtocolParams(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toProtocolParamsDto);
    }

    public EpochNo getLatestEpoch() {
        var latestEpoch = epochParamStorage.getMaxEpoch();
        return new EpochNo(latestEpoch);
    }
}
