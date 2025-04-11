package com.bloxbean.cardano.yaci.store.api.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.AdaPotDto;
import com.bloxbean.cardano.yaci.store.api.adapot.mapper.AdaPotDtoMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdaPotApiService {
    private final AdaPotStorage adaPotStorage;
    private final EpochParamStorage epochParamStorage;
    private final AdaPotDtoMapper adaPotDtoMapper;

    public Optional<AdaPotDto> getAdaPot() {
        int currentEpoch = epochParamStorage.getMaxEpoch();
        return getAdaPot(currentEpoch);
    }

    public Optional<AdaPotDto> getAdaPot(int epoch) {
        return adaPotStorage.findByEpoch(epoch)
                .map( adaPotDtoMapper::toAdaPotDto);
    }
}
