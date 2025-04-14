package com.bloxbean.cardano.yaci.store.api.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.NetworkInfoDto;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NetworkInfoApiService {
    private final EpochStakeStorageReader epochStakeStorage;
    private final AdaPotStorage adaPotStorage;
    private final EpochParamStorage epochParamStorage;
    private final GenesisConfig genesisConfig;

    public Optional<NetworkInfoDto> getNetworkInfo() {
        int currentEpoch = epochParamStorage.getMaxEpoch();
        return getNetworkInfo(currentEpoch);
    }

    public Optional<NetworkInfoDto> getNetworkInfo(int epoch) {
        var adaPotOpt = adaPotStorage.findByEpoch(epoch);

        var maxSupply = genesisConfig.getMaxLovelaceSupply();

        var supply = adaPotOpt.map(adaPot ->
                new NetworkInfoDto.Supply(maxSupply, adaPot.getCirculation(), adaPot.getTreasury(), adaPot.getReserves())
        ).orElse(new NetworkInfoDto.Supply(maxSupply, null, null, null));

        var totalActiveStake = epochStakeStorage.getTotalActiveStakeByEpoch(epoch);

        var stake = new NetworkInfoDto.Stake(totalActiveStake.orElse(null));

        return Optional.of(new NetworkInfoDto(supply, stake));
    }
}
