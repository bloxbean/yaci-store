package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedGovernanceData;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalStateServiceTest {

    @Mock
    private ProposalStateClient proposalStateClient;
    @Mock
    private EpochParamStorage epochParamStorage;
    @Mock
    private CommitteeStorage committeeStorage;
    @Mock
    private CommitteeMemberStorage committeeMemberStorage;
    @Mock
    private AdaPotStorage adaPotStorage;
    @Mock
    private BootstrapPhaseService bootstrapPhaseService;
    @Mock
    private ProposalCollectionService proposalCollectionService;
    @Mock
    private CommitteeStateService committeeStateService;
    @Mock
    private VotingDataCollector votingDataCollector;

    @Test
    void collectGovernanceData_shouldUseCurrentEpochCommitteeMembersForRatification() {
        int currentEpoch = 762;

        GovActionProposal proposal = GovActionProposal.builder()
                .txHash("a84674f5b7f77af7791e452c0b6a11eabb888ed474e635613b211236b942d1a1")
                .index(0)
                .build();
        List<GovActionProposal> proposals = List.of(proposal);

        List<CommitteeMemberDetails> currentCommitteeMembers = List.of(
                CommitteeMemberDetails.builder()
                        .coldKey("33e984fd73ede865bf1e385fe001d08efc99d187b854f69bb04c4aab")
                        .hotKey("20c883612be5e35889df3bac348f2860bbf7524d2fde2b5c41c9f15a")
                        .startEpoch(719)
                        .expiredEpoch(866)
                        .build()
        );

        Committee committee = Committee.builder()
                .epoch(currentEpoch)
                .thresholdNumerator(BigInteger.valueOf(2))
                .thresholdDenominator(BigInteger.valueOf(3))
                .build();

        EpochParam epochParam = EpochParam.builder()
                .epoch(currentEpoch)
                .params(ProtocolParams.builder().build())
                .build();

        when(bootstrapPhaseService.isInConwayBootstrapPhase(currentEpoch)).thenReturn(false);
        when(proposalCollectionService.getProposalsForStatusEvaluation(currentEpoch)).thenReturn(proposals);
        when(votingDataCollector.collectVotingDataBatch(proposals, currentEpoch - 1)).thenReturn(Map.<GovActionId, AggregatedVotingData>of());
        when(epochParamStorage.getProtocolParams(currentEpoch)).thenReturn(Optional.of(epochParam));
        when(committeeStorage.getCommitteeByEpoch(currentEpoch)).thenReturn(Optional.of(committee));
        when(committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(currentEpoch)).thenReturn(currentCommitteeMembers);
        when(committeeStateService.getCurrentCommitteeState()).thenReturn(ConstitutionCommitteeState.NORMAL);
        when(adaPotStorage.findByEpoch(currentEpoch)).thenReturn(Optional.of(AdaPot.builder().epoch(currentEpoch).treasury(BigInteger.TEN).build()));
        when(proposalStateClient.getLastEnactedProposal(any(GovActionType.class), eq(currentEpoch))).thenReturn(Optional.empty());

        AggregatedGovernanceData result = new ProposalStateService(
                proposalStateClient,
                epochParamStorage,
                committeeStorage,
                committeeMemberStorage,
                adaPotStorage,
                bootstrapPhaseService,
                proposalCollectionService,
                committeeStateService,
                votingDataCollector
        ).collectGovernanceData(currentEpoch);

        assertThat(result).isNotNull();
        assertThat(result.committeeMembers()).containsExactlyElementsOf(currentCommitteeMembers);
        verify(votingDataCollector).collectVotingDataBatch(proposals, currentEpoch - 1);
        verify(committeeMemberStorage).getActiveCommitteeMembersDetailsByEpoch(currentEpoch);
        verify(committeeMemberStorage, never()).getActiveCommitteeMembersDetailsByEpoch(currentEpoch - 1);
    }
}
