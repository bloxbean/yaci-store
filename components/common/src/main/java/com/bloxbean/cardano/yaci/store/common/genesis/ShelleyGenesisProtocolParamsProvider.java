package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.GenesisFileUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;

import java.io.File;
import java.util.Optional;

public class ShelleyGenesisProtocolParamsProvider {
    private final Optional<ProtocolParams> protocolParams;

    public ShelleyGenesisProtocolParamsProvider(StoreProperties storeProperties) {
        this.protocolParams = loadProtocolParams(storeProperties);
    }

    public Optional<ProtocolParams> getProtocolParams() {
        return protocolParams;
    }

    private Optional<ProtocolParams> loadProtocolParams(StoreProperties storeProperties) {
        try {
            String shelleyGenesisFile = storeProperties.getShelleyGenesisFile();
            ProtocolParams params;
            if (StringUtil.isEmpty(shelleyGenesisFile)) {
                if (GenesisFileUtil.getGenesisfileDefaultFolder(storeProperties.getProtocolMagic()) == null) {
                    return Optional.empty();
                }
                params = new ShelleyGenesis(storeProperties.getProtocolMagic()).getProtocolParams();
            } else {
                params = new ShelleyGenesis(new File(shelleyGenesisFile)).getProtocolParams();
            }
            return Optional.ofNullable(params);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
