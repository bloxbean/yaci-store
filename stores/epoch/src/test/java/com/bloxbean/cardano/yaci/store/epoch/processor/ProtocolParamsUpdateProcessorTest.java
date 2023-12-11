package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.Update;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.UpdateEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProtocolParamsUpdateProcessorTest {
    @Mock
    private ProtocolParamsProposalStorage protocolParamsProposalStorage;

    @InjectMocks
    private ProtocolParamsUpdateProcessor protocolParamsUpdateProcessor;

    @Captor
    private ArgumentCaptor<List<ProtocolParamsProposal>> argCaptor;


    @Test
    void givenUpdateEvent_whenPpUpdateSizeIsZero_shouldReturn() {
        protocolParamsUpdateProcessor.handleUpdateEvent(UpdateEvent.builder()
                .updates(new ArrayList<>())
                .metadata(EventMetadata.builder()
                        .protocolMagic(1)
                        .build())
                .build());

        Mockito.verify(protocolParamsProposalStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenUpdateEvent_whenTxUpdateIsNull_shouldReturn() {
        protocolParamsUpdateProcessor.handleUpdateEvent(UpdateEvent.builder()
                .updates(null)
                .build());

        Mockito.verify(protocolParamsProposalStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenUpdateEvent_whenProtocolParamsProposalsIsEmpty_shouldDoNotSaveProtocolParamProposal() {
        List<TxUpdate> txUpdates = new ArrayList<>();
        txUpdates.add(TxUpdate.builder()
                        .txHash("a9279f32f7d36320b61074e7abd95651c8c01f0be2b91a06d9d3e99d00d18602")
                        .update(Update.builder()
                                .protocolParamUpdates(new HashMap<>())
                                .build())
                .build());

        protocolParamsUpdateProcessor.handleUpdateEvent(UpdateEvent.builder()
                        .updates(txUpdates)
                        .build());

        Mockito.verify(protocolParamsProposalStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenUpdateEvent_whenProtocolParamsProposalsNotEmpty_shouldSaveProtocolParamsProposals() {
        UpdateEvent updateEvent = UpdateEvent.builder()
                .updates(txUpdateList())
                .metadata(EventMetadata.builder()
                        .epochNumber(28)
                        .slot(10659687)
                        .era(Era.Shelley)
                        .blockTime(1666342887)
                        .block(177070)
                        .build())
                .build();

        protocolParamsUpdateProcessor.handleUpdateEvent(updateEvent);

        Mockito.verify(protocolParamsProposalStorage, Mockito.times(1)).saveAll(argCaptor.capture());

        List<ProtocolParamsProposal> protocolParamsProposals = argCaptor.getValue();

        assertThat(protocolParamsProposals.get(0).getEpoch()).isEqualTo(28);
        assertThat(protocolParamsProposals.get(0).getTargetEpoch()).isEqualTo(28);
        assertThat(protocolParamsProposals.get(0).getSlot()).isEqualTo(10659687);
        assertThat(protocolParamsProposals.get(0).getEra()).isEqualTo(Era.Shelley);
        assertThat(protocolParamsProposals.get(0).getBlockNumber()).isEqualTo(177070);
        assertThat(protocolParamsProposals.get(0).getBlockTime()).isEqualTo(1666342887);
        assertThat(protocolParamsProposals.get(0).getTxHash()).isEqualTo("a9279f32f7d36320b61074e7abd95651c8c01f0be2b91a06d9d3e99d00d18602");
        assertThat(protocolParamsProposals.get(0).getKeyHash()).isEqualTo("dd2a7d71a05bed11db61555ba4c658cb1ce06c8024193d064f2a66ae");
    }

    private List<TxUpdate> txUpdateList() {
        Map<String, ProtocolParamUpdate> ppUpdates = new HashMap<>();

        ppUpdates.put("dd2a7d71a05bed11db61555ba4c658cb1ce06c8024193d064f2a66ae",
                ProtocolParamUpdate.builder().build());

        TxUpdate txUpdate = TxUpdate.builder()
                .update(Update.builder()
                        .epoch(28)
                        .protocolParamUpdates(ppUpdates)
                        .build())
                .txHash("a9279f32f7d36320b61074e7abd95651c8c01f0be2b91a06d9d3e99d00d18602")
                .build();

        List<TxUpdate> txUpdates = new ArrayList<>();
        txUpdates.add(txUpdate);

        return txUpdates;
    }
}
