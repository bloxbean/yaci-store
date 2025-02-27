package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitteeProcessorTest {
    @Mock
    private StoreProperties storeProperties;

    @Mock
    private CommitteeStorage committeeStorage;

    @Mock
    private CommitteeStorageReader committeeStorageReader;

    @Mock
    private ProposalStateClient proposalStateClient;

    @InjectMocks
    private CommitteeProcessor committeeProcessor;

    @Captor
    ArgumentCaptor<Committee> committeeCaptor;

    @Test
    void handleEpochChangeEvent_ShouldUpdateCommittee_WhenProposalEnacted() {
        PreEpochTransitionEvent event = PreEpochTransitionEvent.builder()
                .era(Era.Conway)
                .previousEra(Era.Conway)
                .epoch(101)
                .previousEpoch(100)
                .metadata(EventMetadata
                        .builder()
                        .era(Era.Conway)
                        .epochNumber(101)
                        .slot(6000)
                        .build())
                .build();

        Credential removedMember1 = new Credential(StakeCredType.ADDR_KEYHASH, "aaaaaa06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential removedMember2 = new Credential(StakeCredType.SCRIPTHASH, "bbbbbb06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential newMember1 = new Credential(StakeCredType.ADDR_KEYHASH, "eeeeee06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential newMember2 = new Credential(StakeCredType.SCRIPTHASH, "ffffff06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");

        UpdateCommittee updateCommittee = new UpdateCommittee(null, Set.of(removedMember1, removedMember2),
                Map.of(newMember1, 50, newMember2, 60),
                BigDecimal.valueOf(0.75));

        GovActionProposal proposal = GovActionProposal
                .builder()
                .txHash("acc84cdb5eae8fe33793a9329bdd73b51eef7d9a33f25352783015aaa63c16fe")
                .index(1)
                .govAction(updateCommittee)
                .build();

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, event.getPreviousEpoch()))
                .thenReturn(List.of(proposal));

        committeeProcessor.handleEpochChangeEvent(event);

        Mockito.verify(committeeStorage).save(committeeCaptor.capture());
        Committee savedCommittee = committeeCaptor.getValue();

        assertThat(savedCommittee.getEpoch()).isEqualTo(101);
        assertThat(savedCommittee.getSlot()).isEqualTo(6000);
        assertThat(savedCommittee.getThreshold()).isEqualTo(BigDecimal.valueOf(0.75));
        assertThat(savedCommittee.getGovActionTxHash()).isEqualTo("acc84cdb5eae8fe33793a9329bdd73b51eef7d9a33f25352783015aaa63c16fe");
        assertThat(savedCommittee.getGovActionIndex()).isEqualTo(1);
    }
}
