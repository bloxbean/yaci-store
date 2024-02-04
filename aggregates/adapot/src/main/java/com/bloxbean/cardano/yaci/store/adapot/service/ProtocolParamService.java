package com.bloxbean.cardano.yaci.store.adapot.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
public class ProtocolParamService {

    public BigInteger getKeyDeposit() {
        return adaToLovelace(2);
    }

    public BigInteger getPoolDeposit() {
        return adaToLovelace(500);
    }

    public BigInteger getDRepDeposit() {
        return adaToLovelace(1000);
    }

    public BigInteger getGovActionDeposit() {
        return adaToLovelace(1000);
    }
}
