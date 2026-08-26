package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
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

    @Mock
    private GenesisConfig genesisConfig;

    @Test
    void getKeyDeposit_returnsEpochParamWhenPresent() {
        when(protocolParamService.getProtocolParam(100))
                .thenReturn(Optional.of(ProtocolParams.builder()
                        .keyDeposit(BigInteger.valueOf(3_000_000))
                        .build()));

        DepositParamService service = new DepositParamService(protocolParamService, genesisConfig);

        assertThat(service.getKeyDeposit(100)).isEqualTo(BigInteger.valueOf(3_000_000));
    }

    @Test
    void getKeyDeposit_fallsBackToShelleyGenesisWhenEpochParamsMissing() {
        when(protocolParamService.getProtocolParam(100)).thenReturn(Optional.empty());
        when(genesisConfig.getShelleyGenesisProtocolParams())
                .thenReturn(ProtocolParams.builder()
                        .keyDeposit(BigInteger.valueOf(2_000_000))
                        .build());

        DepositParamService service = new DepositParamService(protocolParamService, genesisConfig);

        assertThat(service.getKeyDeposit(100)).isEqualTo(BigInteger.valueOf(2_000_000));
    }

    @Test
    void getKeyDeposit_fallsBackToTwoAdaWhenGenesisMissing() {
        when(genesisConfig.getShelleyGenesisProtocolParams()).thenReturn(null);

        DepositParamService service = new DepositParamService(null, genesisConfig);

        assertThat(service.getKeyDeposit(0)).isEqualTo(adaToLovelace(2));
    }

}
