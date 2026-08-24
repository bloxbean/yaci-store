package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.util.GenesisFileUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
public class DepositParamService {
    private final ProtocolParamService protocolParamService;
    private final StoreProperties storeProperties;

    public DepositParamService(@Autowired(required = false) ProtocolParamService protocolParamService,
                               StoreProperties storeProperties) {
        this.protocolParamService = protocolParamService;
        this.storeProperties = storeProperties;
    }

    public BigInteger getKeyDeposit(int epoch) {
        return resolveDeposit(epoch, ProtocolParams::getKeyDeposit, adaToLovelace(2));
    }

    public BigInteger getPoolDeposit(int epoch) {
        return resolveDeposit(epoch, ProtocolParams::getPoolDeposit, adaToLovelace(500));
    }

    public BigInteger getDRepDeposit(int epoch) {
        return adaToLovelace(1000);
    }

    public BigInteger getGovActionDeposit(int epoch) {
        return adaToLovelace(1000);
    }

    private BigInteger resolveDeposit(int epoch, Function<ProtocolParams, BigInteger> getter, BigInteger hardcodedFallback) {
        if (protocolParamService != null) {
            BigInteger fromEpoch = protocolParamService.getProtocolParam(epoch)
                    .map(getter)
                    .orElse(null);
            if (fromEpoch != null) {
                return fromEpoch;
            }
        }

        return getShelleyGenesisProtocolParams()
                .map(getter)
                .orElse(hardcodedFallback);
    }

    private Optional<ProtocolParams> getShelleyGenesisProtocolParams() {
        try {
            String shelleyGenesisFile = storeProperties.getShelleyGenesisFile();
            ProtocolParams protocolParams;
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
