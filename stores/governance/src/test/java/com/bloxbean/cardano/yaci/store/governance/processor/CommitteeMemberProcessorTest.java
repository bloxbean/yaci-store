package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitteeMemberProcessorTest {

    @Mock
    private StoreProperties storeProperties;

    @Mock
    private CommitteeMemberStorage committeeMemberStorage;

    @Mock
    private CommitteeMemberStorageReader committeeMemberStorageReader;

    @Mock
    private ProposalStateClient proposalStateClient;

    @InjectMocks
    private CommitteeMemberProcessor committeeMemberProcessor;

    @Captor
    ArgumentCaptor<List<CommitteeMember>> committeeMembersCaptor;

    @Test
    void handlePreAdaPotJobProcessingEvent_ShouldUpdateCommitteeMembers_WhenProposalEnacted() {
        PreAdaPotJobProcessingEvent event = PreAdaPotJobProcessingEvent.builder()
                .epoch(101)
                .slot(6000)
                .block(500)
                .build();

        Credential removedMember1 = new Credential(StakeCredType.ADDR_KEYHASH, "aaaaaa06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential removedMember2 = new Credential(StakeCredType.SCRIPTHASH, "bbbbbb06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential newMember1 = new Credential(StakeCredType.ADDR_KEYHASH, "eeeeee06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential newMember2 = new Credential(StakeCredType.SCRIPTHASH, "ffffff06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");

        UpdateCommittee updateCommittee = new UpdateCommittee(null, Set.of(removedMember1, removedMember2),
                Map.of(newMember1, 150, newMember2, 160),
                UnitInterval.fromString("75/100"));

        GovActionProposal proposal = GovActionProposal
                .builder()
                .epoch(100)
                .govAction(updateCommittee)
                .build();

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 100))
                .thenReturn(List.of(proposal));
        when(proposalStateClient.getLastEnactedProposal(GovActionType.UPDATE_COMMITTEE, 100))
                .thenReturn(Optional.empty());
        when(proposalStateClient.getLastEnactedProposal(GovActionType.NO_CONFIDENCE, 100))
                .thenReturn(Optional.empty());

        List<CommitteeMember> existingMembers = List.of(
                CommitteeMember.builder()
                        .credType(CredentialType.ADDR_KEYHASH)
                        .startEpoch(80)
                        .expiredEpoch(199)
                        .hash("aaaaaa06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b").build(),
                CommitteeMember.builder()
                        .credType(CredentialType.SCRIPTHASH)
                        .startEpoch(80)
                        .expiredEpoch(199)
                        .hash("bbbbbb06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b").build(),
                CommitteeMember.builder()
                        .credType(CredentialType.SCRIPTHASH)
                        .startEpoch(80)
                        .expiredEpoch(200)
                        .hash("cccccc06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b").build(),
                CommitteeMember.builder()
                        .credType(CredentialType.SCRIPTHASH)
                        .startEpoch(80)
                        .expiredEpoch(200)
                        .hash("dddddd06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b").build()
        );

        when(committeeMemberStorage.getCommitteeMembersByEpoch(100)).thenReturn(existingMembers);

        committeeMemberProcessor.handlePreAdaPotJobProcessingEvent(event);

        //verify
        Mockito.verify(committeeMemberStorage).saveAll(committeeMembersCaptor.capture());
        List<CommitteeMember> savedMembers = committeeMembersCaptor.getAllValues().get(0);
        assertThat(savedMembers).hasSize(4);

        assertThat(savedMembers.stream().anyMatch(member -> member.getHash().equals("cccccc06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                && member.getStartEpoch() == 80
                && member.getExpiredEpoch() == 200
                && member.getCredType() == CredentialType.SCRIPTHASH
                && member.getSlot() == 6000L))
                .isTrue();
        assertThat(savedMembers.stream().anyMatch(member -> member.getHash().equals("dddddd06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                && member.getStartEpoch() == 80
                && member.getExpiredEpoch() == 200
                && member.getCredType() == CredentialType.SCRIPTHASH
                && member.getSlot() == 6000L))
                .isTrue();

        assertThat(savedMembers.stream().anyMatch(member -> member.getHash().equals("eeeeee06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                && member.getStartEpoch() == 101
                && member.getExpiredEpoch() == 150
                && member.getCredType() == CredentialType.ADDR_KEYHASH
                && member.getSlot() == 6000L))
                .isTrue();

        assertThat(savedMembers.stream().anyMatch(member -> member.getHash().equals("ffffff06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                && member.getStartEpoch() == 101
                && member.getExpiredEpoch() == 160
                && member.getCredType() == CredentialType.SCRIPTHASH
                && member.getSlot() == 6000L))
                .isTrue();

    }

    @Test
    void handlePreAdaPotJobProcessingEvent_ShouldResetCommitteeMembers_WhenNoConfidenceWasLatestCommitteeAction() {
        PreAdaPotJobProcessingEvent event = PreAdaPotJobProcessingEvent.builder()
                .epoch(101)
                .slot(6000)
                .block(500)
                .build();

        Credential newMember1 = new Credential(StakeCredType.ADDR_KEYHASH, "eeeeee06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");
        Credential newMember2 = new Credential(StakeCredType.SCRIPTHASH, "ffffff06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b");

        UpdateCommittee updateCommittee = new UpdateCommittee(null, Set.of(),
                Map.of(newMember1, 150, newMember2, 160),
                UnitInterval.fromString("75/100"));

        GovActionProposal enactedUpdateProposal = GovActionProposal
                .builder()
                .epoch(100)
                .slot(5000L)
                .govAction(updateCommittee)
                .build();

        GovActionProposal lastUpdateCommittee = GovActionProposal.builder()
                .epoch(90)
                .slot(4000L)
                .govAction(new UpdateCommittee(null, Set.of(), Map.of(), UnitInterval.fromString("50/100")))
                .build();

        GovActionProposal lastNoConfidence = GovActionProposal.builder()
                .epoch(95)
                .slot(4500L)
                .govAction(new NoConfidence(null))
                .build();

        when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, 100))
                .thenReturn(List.of(enactedUpdateProposal));
        when(proposalStateClient.getLastEnactedProposal(GovActionType.UPDATE_COMMITTEE, 100))
                .thenReturn(Optional.of(lastUpdateCommittee));
        when(proposalStateClient.getLastEnactedProposal(GovActionType.NO_CONFIDENCE, 100))
                .thenReturn(Optional.of(lastNoConfidence));

        when(committeeMemberStorage.getCommitteeMembersByEpoch(100)).thenReturn(List.of(
                CommitteeMember.builder()
                        .credType(CredentialType.ADDR_KEYHASH)
                        .startEpoch(80)
                        .expiredEpoch(199)
                        .hash("aaaaaa06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                        .build()
        ));

        committeeMemberProcessor.handlePreAdaPotJobProcessingEvent(event);

        Mockito.verify(committeeMemberStorage).saveAll(committeeMembersCaptor.capture());
        List<CommitteeMember> savedMembers = committeeMembersCaptor.getValue();

        assertThat(savedMembers).hasSize(2);
        assertThat(savedMembers)
                .allMatch(member -> member.getHash().equals("eeeeee06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")
                        || member.getHash().equals("ffffff06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b"));
        assertThat(savedMembers.stream().anyMatch(member -> member.getHash().equals("aaaaaa06fd4e8f51062dc431362369b2a43140abced8aa2ff2256d7b")))
                .isFalse();
    }
}
