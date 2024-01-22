package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.AlonzoGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
//TODO -- Write tests
public class EraGenesisProtocolParamsUtil {
    private final StoreProperties storeProperties;

    public Optional<ProtocolParams> getGenesisProtocolParameters(Era newEra, Era prevEra, long protocolMagic) {
        ProtocolParams genesisProtocolParams = null;
        if (newEra != prevEra) {
            //Get default protocol params if any
            if (newEra == Era.Byron) {
                //Do nothing
            } else if (newEra == Era.Shelley) {
                genesisProtocolParams = getShelleyGenesisProtocolParams(protocolMagic);
            } else if (newEra == Era.Alonzo) {
                genesisProtocolParams = getAlonzoGenesisProtocolParams(protocolMagic);
            } else if (newEra == Era.Babbage) {
                //Starting with Babbage era at slot = 0, so merge alonzo and shelley params and apply required rules
                if (prevEra == null) { //Looks like it's a custom network. Let's populate with default params
                    ProtocolParams shelleyPP = getShelleyGenesisProtocolParams(protocolMagic);
                    ProtocolParams alonzoPP = getAlonzoGenesisProtocolParams(protocolMagic);

                    genesisProtocolParams = new ProtocolParams();
                    genesisProtocolParams.merge(shelleyPP);
                    genesisProtocolParams.merge(alonzoPP);
                }
            } else if (newEra == Era.Conway) {
                //Starting with Babbage era at slot = 0, so merge alonzo and shelley params and apply required rules
                if (prevEra == null) { //Looks like it's a custom network. Let's populate with default params
                    ProtocolParams shelleyPP = getShelleyGenesisProtocolParams(protocolMagic);
                    ProtocolParams alonzoPP = getAlonzoGenesisProtocolParams(protocolMagic);
                    ProtocolParams conwayPP = getConwayGenesisProtocolParams(protocolMagic);

                    genesisProtocolParams = new ProtocolParams();
                    genesisProtocolParams.merge(shelleyPP);
                    genesisProtocolParams.merge(alonzoPP);
                    genesisProtocolParams.merge(conwayPP);
                } else {
                    genesisProtocolParams = getConwayGenesisProtocolParams(protocolMagic);
                }
            } else {
                log.warn("No genesis protocol parameters handled for era {}", newEra);
            }
        }
        return Optional.ofNullable(genesisProtocolParams);
    }

    private ProtocolParams getShelleyGenesisProtocolParams(long protocolMagic) {
        String shelleyGenesisFile = storeProperties.getShelleyGenesisFile();
        if (StringUtil.isEmpty(shelleyGenesisFile))
            return new ShelleyGenesis(protocolMagic).getProtocolParams();
        else
            return new ShelleyGenesis(new File(shelleyGenesisFile)).getProtocolParams();
    }

    private ProtocolParams getAlonzoGenesisProtocolParams(long protocolMagic) {
        String alonzoGenesisFile = storeProperties.getAlonzoGenesisFile();
        if (StringUtil.isEmpty(alonzoGenesisFile))
            return new AlonzoGenesis(protocolMagic).getProtocolParams();
        else
            return new AlonzoGenesis(new File(alonzoGenesisFile)).getProtocolParams();
    }

    private ProtocolParams getConwayGenesisProtocolParams(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();
        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic).getProtocolParams();
        else
            return new ConwayGenesis(new File(conwayGenesisFile)).getProtocolParams();
    }
}
