package com.bloxbean.cardano.yaci.store.api.epoch.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.api.epoch.dto.EpochDto;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
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
    private final EraService eraService;
    private final DomainMapper mapper = DomainMapper.INSTANCE;

    public Optional<ProtocolParamsDto> getLatestProtocolParams() {
        int epoch = epochParamStorage.getMaxEpoch();
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toProtocolParamsDto)
                .map(protocolParamsDto -> {
                    System.out.println("Setting epoch in param dto");
                    protocolParamsDto.setEpoch(epoch);
                    return protocolParamsDto;
                });
    }

    public Optional<ProtocolParamsDto> getProtocolParams(int epoch) {
        return epochParamStorage.getProtocolParams(epoch)
                .map(EpochParam::getParams)
                .map(mapper::toProtocolParamsDto)
                .map(protocolParamsDto -> {
                    System.out.println("Setting epoch in param dto");
                    protocolParamsDto.setEpoch(epoch);
                    return protocolParamsDto;
                });
    }

    public int getLatestEpoch() {
        var latestEpoch = epochParamStorage.getMaxEpoch();
        return latestEpoch;
    }

    public EpochDto getEpochDetails(int epoch) {
        var firstNonByronEpoch = eraService.getFirstNonByronEpoch()
                .orElse(0);

        if (epoch >= firstNonByronEpoch) { //shelley and post shelley epochs
            //We are using Era.Shelley as it represents all non-byron era in this case
            var startAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch, 0);
            var endAbsoluteSlot = eraService.getShelleyAbsoluteSlot(epoch, (int) eraService.slotsPerEpoch(Era.Shelley));

            long startTime = eraService.blockTime(Era.Shelley, startAbsoluteSlot);
            long endTime = eraService.blockTime(Era.Shelley, endAbsoluteSlot);

            return EpochDto.builder()
                    .epoch(epoch)
                    .startTime(startTime)
                    .endTime(endTime)
                    .output("0")
                    .fees("0")
                    .activeStake("0")
                    .build();
        } else { //Byron epoch. Just return epoch no for now
            return EpochDto.builder()
                    .epoch(epoch)
                    .output("0")
                    .fees("0")
                    .activeStake("0")
                    .build();
        }
    }
}
