package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.governance.Anchor;
import com.bloxbean.cardano.yaci.core.model.governance.Constitution;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstitutionProcessorTest {

    @Mock
    private StoreProperties storeProperties;

    @Mock
    private ConstitutionStorage constitutionStorage;

    @Mock
    private ConstitutionStorageReader constitutionStorageReader;

    @Mock
    private ProposalStateClient proposalStateClient;

    @InjectMocks
    private ConstitutionProcessor constitutionProcessor;

    @Captor
    ArgumentCaptor<com.bloxbean.cardano.yaci.store.governance.domain.Constitution> constitutionCaptor;

    @Test
    void handlePreAdaPotJobProcessingEvent_ShouldUpdateConstitution_WhenProposalEnacted() {
        PreAdaPotJobProcessingEvent event = PreAdaPotJobProcessingEvent.builder()
                .epoch(101)
                .slot(6000)
                .block(500)
                .build();

        NewConstitution newConstitution = NewConstitution.builder()
                .constitution(Constitution.builder()
                        .anchor(Anchor
                                .builder()
                                .anchor_url("https://bit.ly/3zCH2HL")
                                .anchor_data_hash("cd4ca39b59b7c1133635fb97668bde1f28934af4dfd524fdeacda0d47ffb8bf2")
                                .build())
                        .scripthash("fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64")
                        .build())
                .build();
        GovActionProposal proposal = GovActionProposal
                .builder()
                .govAction(newConstitution)
                .build();
        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 100))
                .thenReturn(List.of(proposal));

        constitutionProcessor.handlePreAdaPotJobProcessingEvent(event);

        Mockito.verify(constitutionStorage).save(constitutionCaptor.capture());
        com.bloxbean.cardano.yaci.store.governance.domain.Constitution savedConstitution = constitutionCaptor.getValue();
        assertThat(savedConstitution.getScript()).isEqualTo("fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64");
        assertThat(savedConstitution.getAnchorUrl()).isEqualTo("https://bit.ly/3zCH2HL");
        assertThat(savedConstitution.getAnchorHash()).isEqualTo("cd4ca39b59b7c1133635fb97668bde1f28934af4dfd524fdeacda0d47ffb8bf2");
        assertThat(savedConstitution.getSlot()).isEqualTo(6000);
        assertThat(savedConstitution.getActiveEpoch()).isEqualTo(101);
    }
}

