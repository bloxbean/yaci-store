package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EpochParamProcessorTest {

    @Mock
    private EpochParamStorage epochParamStorage;

    @Mock
    private ProtocolParamsProposalStorage protocolParamsProposalStorage;

    @Mock
    private EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;

    @InjectMocks
    private EpochParamProcessor epochParamProcessor;

    @Captor
    private ArgumentCaptor<EpochParam> argCaptor;

    EpochParamProcessorTest() {
    }

    @Test
    void givenEpochChangeEvent_whenPreviousEpochIsNullAndEpochEqualsMaxEpoch_shouldReturn() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(null)
                .era(Era.Byron)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        Mockito.when(epochParamStorage.getMaxEpoch()).thenReturn(28);

        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);
        Mockito.verify(epochParamStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenEpochChangeEvent_whenMaxEpochIsNotNullAndMaxEpochPlusOneIsNotEqualToEpoch_shouldReturn() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(28)
                .era(Era.Shelley)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        Mockito.when(epochParamStorage.getMaxEpoch()).thenReturn(30);

        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);
        Mockito.verify(epochParamStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenEpochChangeEvent_shouldSaveEpochParam() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(27)
                .era(Era.Alonzo)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        Mockito.when(epochParamStorage.getMaxEpoch()).thenReturn(27);
        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);

        Mockito.verify(epochParamStorage, Mockito.times(1)).save(argCaptor.capture());

        Mockito.verify(epochParamStorage).getProtocolParams(27);
        Mockito.verify(protocolParamsProposalStorage).getProtocolParamsProposalsByTargetEpoch(28);
        Mockito.verify(protocolParamsProposalStorage).getProtocolParamsProposalsByTargetEpoch(27);

        EpochParam epochParam = argCaptor.getValue();

        assertThat(epochParam.getEpoch()).isEqualTo(28);
        assertThat(epochParam.getSlot()).isEqualTo(12961);
        assertThat(epochParam.getBlockNumber()).isEqualTo(177070);
        assertThat(epochParam.getBlockTime()).isEqualTo(1666342887);
    }
}
