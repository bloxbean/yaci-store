package com.bloxbean.cardano.yaci.store.client.epoch;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.ccl.CclProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.util.GenesisFileUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;

@Slf4j
public class GenesisEpochParamClient implements EpochParamClient {
    private final StoreProperties storeProperties;

    public GenesisEpochParamClient(StoreProperties storeProperties) {
        this.storeProperties = storeProperties;
        log.warn("Genesis Epoch Param Client Configured >>>>>>");
    }

    @Override
    public Optional<ProtocolParams> getLatestProtocolParams() {
        return getShelleyGenesisProtocolParams()
                .map(CclProtocolParamsMapper::toCclProtocolParams);
    }

    private Optional<com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams> getShelleyGenesisProtocolParams() {
        try {
            String shelleyGenesisFile = storeProperties.getShelleyGenesisFile();
            com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams protocolParams;
            if (StringUtil.isEmpty(shelleyGenesisFile)) {
                if (GenesisFileUtil.getGenesisfileDefaultFolder(storeProperties.getProtocolMagic()) == null) {
                    return Optional.empty();
                }
                protocolParams = new ShelleyGenesis(storeProperties.getProtocolMagic()).getProtocolParams();
            } else {
                protocolParams = new ShelleyGenesis(new File(shelleyGenesisFile)).getProtocolParams();
            }
            return Optional.ofNullable(protocolParams);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
