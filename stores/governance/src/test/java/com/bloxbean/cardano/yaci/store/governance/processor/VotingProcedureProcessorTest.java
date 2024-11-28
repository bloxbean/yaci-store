package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.governance.*;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingProcedureProcessorTest {
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private VotingProcedureStorage votingProcedureStorage;
    @Captor
    private ArgumentCaptor<List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> votingProceduresCaptor;
    @InjectMocks
    private VotingProcedureProcessor votingProcedureProcessor;

    @Test
    void givenGovernanceEvent_WhenNotExistsVoting_ShouldNotSaveAnything() {
        final GovernanceEvent governanceEvent = GovernanceEvent.builder()
                .metadata(eventMetadata())
                .txGovernanceList(List.of(
                        TxGovernance.builder()
                                .txHash("498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f")
                                .votingProcedures(VotingProcedures.builder()
                                        .voting(Map.of())
                                        .build())
                                .build(),
                        TxGovernance.builder()
                                .txHash("498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f")
                                .votingProcedures(null)
                                .build()
                ))
                .build();

        votingProcedureProcessor.handleVotingProcedure(governanceEvent);

        verify(votingProcedureStorage, never()).saveAll(any());
    }

    @Test
    void givenGovernanceEvent_ShouldHandleGovernanceEventAndSaveVotingProcedures() {
        final GovernanceEvent governanceEvent = GovernanceEvent.builder()
                .metadata(eventMetadata())
                .txGovernanceList(List.of(
                        TxGovernance.builder()
                                .txHash("498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f")
                                .votingProcedures(VotingProcedures.builder()
                                        .voting(voting())
                                        .build())
                                .build()
                ))
                .build();

        votingProcedureProcessor.handleVotingProcedure(governanceEvent);

        verify(votingProcedureStorage, times(1)).saveAll(votingProceduresCaptor.capture());

        List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> votingProceduresSaved =
                votingProceduresCaptor.getValue();
        assertThat(votingProceduresSaved).hasSize(1);

        assertThat(votingProceduresSaved.get(0).getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(votingProceduresSaved.get(0).getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(votingProceduresSaved.get(0).getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
        assertThat(votingProceduresSaved.get(0).getEpoch()).isEqualTo(eventMetadata().getEpochNumber());

        assertThat(votingProceduresSaved.get(0).getTxHash()).isEqualTo("498e6ee7063e94c6f459257a89cbd0bc953c5409e6b24460900cc997ba9d1f2f");
        assertThat(votingProceduresSaved.get(0).getVote()).isEqualTo(Vote.NO);
        assertThat(votingProceduresSaved.get(0).getAnchorUrl()).isEqualTo("https://bit.ly/3zCH2HL");
        assertThat(votingProceduresSaved.get(0).getAnchorHash()).isEqualTo("1111111111111111111111111111111111111111111111111111111111111111");
        assertThat(votingProceduresSaved.get(0).getVoterType()).isEqualTo(VoterType.DREP_KEY_HASH);
        assertThat(votingProceduresSaved.get(0).getVoterHash()).isEqualTo("b6fed2a7a3b0ea969b5e9371953b8e7199230545a853990a249872b2");
        assertThat(votingProceduresSaved.get(0).getIndex()).isEqualTo(0);
        assertThat(votingProceduresSaved.get(0).getGovActionTxHash()).isEqualTo("3c282d000dc7763f48270910ea0a47ee893c73f52362f3b036082fd47c94348d");
        assertThat(votingProceduresSaved.get(0).getGovActionIndex()).isEqualTo(1);
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .block(100L)
                .blockTime(99999L)
                .slot(10000L)
                .build();
    }

    private Map<Voter, Map<GovActionId, VotingProcedure>> voting() {
        Map<Voter, Map<GovActionId, VotingProcedure>> votingMap = new LinkedHashMap<>();
        votingMap.put(
                Voter.builder()
                        .type(VoterType.DREP_KEY_HASH)
                        .hash("b6fed2a7a3b0ea969b5e9371953b8e7199230545a853990a249872b2")
                        .build(),
                Map.ofEntries(
                        Map.entry(
                                GovActionId.builder()
                                        .transactionId("3c282d000dc7763f48270910ea0a47ee893c73f52362f3b036082fd47c94348d")
                                        .gov_action_index(1).build(), VotingProcedure.builder().vote(Vote.NO)
                                        .anchor(Anchor.builder()
                                                .anchor_url("https://bit.ly/3zCH2HL")
                                                .anchor_data_hash("1111111111111111111111111111111111111111111111111111111111111111")
                                                .build()).build())));
        return votingMap;
    }
}
