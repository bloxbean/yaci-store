package com.bloxbean.cardano.yaci.store.api.epoch.service;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

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
    private ObjectProvider<EpochNonceStorageReader> epochNonceStorageReaderProvider;

    @Mock
    private EpochNonceStorageReader epochNonceStorageReader;

    private EpochParamService epochParamService;

    @BeforeEach
    void setUp() {
        epochParamService = new EpochParamService(epochParamStorage, eraService, epochNonceStorageReaderProvider);
    }

    @Test
    void getProtocolParams_setsNonceFromEpochNonceStore() {
        givenEpochParam(100);
        when(epochNonceStorageReaderProvider.getIfAvailable()).thenReturn(epochNonceStorageReader);
        when(epochNonceStorageReader.findByEpoch(100)).thenReturn(Optional.of(EpochNonce.builder().epoch(100).nonce(NONCE).build()));

        ProtocolParamsDto dto = epochParamService.getProtocolParams(100).orElseThrow();

        assertThat(dto.getEpoch()).isEqualTo(100);
        assertThat(dto.getNonce()).isEqualTo(NONCE);
    }

    @Test
    void getLatestProtocolParams_setsNonceForMaxEpoch() {
        when(epochParamStorage.getMaxEpoch()).thenReturn(101);
        givenEpochParam(101);
        when(epochNonceStorageReaderProvider.getIfAvailable()).thenReturn(epochNonceStorageReader);
        when(epochNonceStorageReader.findByEpoch(101)).thenReturn(Optional.of(EpochNonce.builder().epoch(101).nonce(NONCE).build()));

        ProtocolParamsDto dto = epochParamService.getLatestProtocolParams().orElseThrow();

        assertThat(dto.getEpoch()).isEqualTo(101);
        assertThat(dto.getNonce()).isEqualTo(NONCE);
    }

    @Test
    void getProtocolParams_nonceNullWhenNoNonceRow() {
        givenEpochParam(100);
        when(epochNonceStorageReaderProvider.getIfAvailable()).thenReturn(epochNonceStorageReader);
        when(epochNonceStorageReader.findByEpoch(100)).thenReturn(Optional.empty());

        assertThat(epochParamService.getProtocolParams(100).orElseThrow().getNonce()).isNull();
    }

    @Test
    void getProtocolParams_nonceNullWhenEpochNonceStoreDisabled() {
        givenEpochParam(100);
        when(epochNonceStorageReaderProvider.getIfAvailable()).thenReturn(null);

        assertThat(epochParamService.getProtocolParams(100).orElseThrow().getNonce()).isNull();
    }

    private void givenEpochParam(int epoch) {
        EpochParam epochParam = EpochParam.builder()
                .epoch(epoch)
                .params(ProtocolParams.builder().minFeeA(44).build())
                .build();
        when(epochParamStorage.getProtocolParams(epoch)).thenReturn(Optional.of(epochParam));
    }
}
