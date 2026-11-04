package com.bloxbean.cardano.yaci.store.api.epoch.service;

import com.bloxbean.cardano.yaci.store.client.epoch.DummyEpochNonceClient;
import com.bloxbean.cardano.yaci.store.client.epoch.EpochNonceClient;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpochParamServiceTest {

    private static final String NONCE = "f2a2eb8e5d6d3d0c4f3b9e0d1c2b3a49586776655443322110ffeeddccbbaa99";

    @Mock
    private EpochParamStorage epochParamStorage;

    @Mock
    private EraService eraService;

    @Mock
    private EpochNonceClient epochNonceClient;

    @Test
    void getProtocolParams_setsNonceFromEpochNonceClient() {
        givenEpochParam(100);
        when(epochNonceClient.getEpochNonce(100)).thenReturn(Optional.of(NONCE));

        ProtocolParamsDto dto = service(epochNonceClient).getProtocolParams(100).orElseThrow();

        assertThat(dto.getEpoch()).isEqualTo(100);
        assertThat(dto.getNonce()).isEqualTo(NONCE);
    }

    @Test
    void getLatestProtocolParams_setsNonceForMaxEpoch() {
        when(epochParamStorage.getMaxEpoch()).thenReturn(101);
        givenEpochParam(101);
        when(epochNonceClient.getEpochNonce(101)).thenReturn(Optional.of(NONCE));

        ProtocolParamsDto dto = service(epochNonceClient).getLatestProtocolParams().orElseThrow();

        assertThat(dto.getEpoch()).isEqualTo(101);
        assertThat(dto.getNonce()).isEqualTo(NONCE);
    }

    @Test
    void getProtocolParams_nonceNullWhenNoNonceForEpoch() {
        givenEpochParam(100);
        when(epochNonceClient.getEpochNonce(100)).thenReturn(Optional.empty());

        assertThat(service(epochNonceClient).getProtocolParams(100).orElseThrow().getNonce()).isNull();
    }

    @Test
    void getProtocolParams_nonceNullWithDummyClient() {
        givenEpochParam(100);

        assertThat(service(new DummyEpochNonceClient()).getProtocolParams(100).orElseThrow().getNonce()).isNull();
    }

    private EpochParamService service(EpochNonceClient client) {
        return new EpochParamService(epochParamStorage, eraService, client);
    }

    private void givenEpochParam(int epoch) {
        EpochParam epochParam = EpochParam.builder()
                .epoch(epoch)
                .params(ProtocolParams.builder().minFeeA(44).build())
                .build();
        when(epochParamStorage.getProtocolParams(epoch)).thenReturn(Optional.of(epochParam));
    }
}
