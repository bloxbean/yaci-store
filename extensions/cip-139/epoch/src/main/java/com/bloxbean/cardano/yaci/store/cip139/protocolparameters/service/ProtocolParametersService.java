package com.bloxbean.cardano.yaci.store.cip139.protocolparameters.service;

import com.bloxbean.cardano.yaci.store.cip139.protocolparameters.dto.ProtocolParametersDto;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProtocolParametersService {

    private final EpochParamStorage epochParamStorage;

    public Optional<ProtocolParametersDto> getLatestProtocolParams() {
        int epoch = epochParamStorage.getMaxEpoch();
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(ProtocolParametersDto::fromDomain);
    }

    public Optional<ProtocolParametersDto> getProtocolParams(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(ProtocolParametersDto::fromDomain);
    }

}
