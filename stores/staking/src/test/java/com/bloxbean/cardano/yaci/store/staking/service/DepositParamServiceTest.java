package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.service.ProtocolParamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Optional;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositParamServiceTest {

    @Mock
    private ProtocolParamService protocolParamService;

    @Test
    void getKeyDeposit_returnsEpochParamWhenPresent() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.MAINNET.getProtocolMagic());
        when(protocolParamService.getProtocolParam(100))
                .thenReturn(Optional.of(ProtocolParams.builder()
                        .keyDeposit(BigInteger.valueOf(3_000_000))
                        .build()));

        DepositParamService service = new DepositParamService(protocolParamService, storeProperties);

        assertThat(service.getKeyDeposit(100)).isEqualTo(BigInteger.valueOf(3_000_000));
    }

    @Test
    void getKeyDeposit_fallsBackToShelleyGenesisWhenEpochParamsMissing() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.MAINNET.getProtocolMagic());
        when(protocolParamService.getProtocolParam(100)).thenReturn(Optional.empty());

        DepositParamService service = new DepositParamService(protocolParamService, storeProperties);

        assertThat(service.getKeyDeposit(100)).isEqualTo(BigInteger.valueOf(2_000_000));
    }

    @Test
    void getKeyDeposit_fallsBackToTwoAdaWhenGenesisMissing() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(42);

        DepositParamService service = new DepositParamService(null, storeProperties);

        assertThat(service.getKeyDeposit(0)).isEqualTo(adaToLovelace(2));
    }
}
