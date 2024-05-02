package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
@RequiredArgsConstructor
public class DepositParamService {
    private final ProtocolParamService protocolParamService;

    public BigInteger getKeyDeposit(int epoch) {
        return protocolParamService.getProtocolParam(epoch)
                .map(p -> p.getKeyDeposit())
                .orElse(adaToLovelace(2));
    }

    public BigInteger getPoolDeposit(int epoch) {
        return protocolParamService.getProtocolParam(epoch)
                .map(p -> p.getPoolDeposit())
                .orElse(adaToLovelace(500));
    }

    public BigInteger getDRepDeposit(int epoch) {
        return adaToLovelace(1000);
    }

    public BigInteger getGovActionDeposit(int epoch) {
        return adaToLovelace(1000);
    }
}
