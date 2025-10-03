package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.processor.EraGenesisProtocolParamsUtil;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BootstrapPhaseService {
    
    private final EpochParamStorage epochParamStorage;
    private final EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;
    private final StoreProperties storeProperties;

    public boolean isInConwayBootstrapPhase(int epoch) {
        boolean result = true;

        if (isPublicNetwork()) {
            Optional<EpochParam> epochParamOpt = epochParamStorage.getProtocolParams(epoch);

            if (epochParamOpt.isPresent()) {
                var protocolParams = epochParamOpt.get().getParams();
                if (protocolParams.getProtocolMajorVer() >= 10) {
                    result = false;
                }
            }
        } else {
            ProtocolParams genesisProtocolParams = eraGenesisProtocolParamsUtil
                    .getGenesisProtocolParameters(Era.Conway, null, storeProperties.getProtocolMagic())
                    .orElse(null);

            if (genesisProtocolParams != null && genesisProtocolParams.getProtocolMajorVer() >= 10) {
                result = false;
            }
        }

        return result;
    }

    private boolean isPublicNetwork() {
        return storeProperties.getProtocolMagic() == Networks.mainnet().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preprod().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preview().getProtocolMagic();
    }
}