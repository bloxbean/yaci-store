package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.function.Function;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
public class DepositParamService {
    private final ProtocolParamService protocolParamService;
    private final GenesisConfig genesisConfig;

    public DepositParamService(@Autowired(required = false) ProtocolParamService protocolParamService,
                               GenesisConfig genesisConfig) {
        this.protocolParamService = protocolParamService;
        this.genesisConfig = genesisConfig;
    }

    public BigInteger getKeyDeposit(int epoch) {
        return resolveDeposit(epoch, ProtocolParams::getKeyDeposit, adaToLovelace(2));
    }

    public BigInteger getPoolDeposit(int epoch) {
        return resolveDeposit(epoch, ProtocolParams::getPoolDeposit, adaToLovelace(500));
    }

    private BigInteger resolveDeposit(int epoch, Function<ProtocolParams, BigInteger> getter, BigInteger legacyFallback) {
        if (protocolParamService != null) {
            BigInteger fromEpoch = protocolParamService.getProtocolParam(epoch)
                    .map(getter)
                    .orElse(null);
            if (fromEpoch != null) {
                return fromEpoch;
            }
        }

        ProtocolParams genesisParams = genesisConfig.getShelleyGenesisProtocolParams();
        if (genesisParams != null) {
            BigInteger fromGenesis = getter.apply(genesisParams);
            if (fromGenesis != null) {
                return fromGenesis;
            }
        }

        // Preserve the pre-existing defaults when epoch and Shelley genesis parameters are unavailable.
        return legacyFallback;
    }
}
