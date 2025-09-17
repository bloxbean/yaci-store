package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedGovernanceData;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationInput;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.ProposalCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GovernanceEvaluationInputMapper {

    private final ProposalCollectionService proposalCollectionService;

    public GovernanceEvaluationInput toGovernanceEvaluationInput(AggregatedGovernanceData data) {
        List<ProposalContext> proposalContexts = proposalCollectionService
                .createProposalContexts(data.proposalsForEvaluation(), data.aggregatedVotingDataByProposal());

        List<CommitteeMember> members = data.committeeMembers().stream()
                .map(this::mapToCommitteeMember)
                .toList();

        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .members(members)
                .threshold(new UnitInterval(data.committeeThresholdNumerator(), data.committeeThresholdDenominator()))
                .build();

        return GovernanceEvaluationInput.builder()
                .currentProposals(proposalContexts)
                .currentEpoch(data.currentEpoch())
                .protocolParams(data.protocolParams())
                .committee(committee)
                .isBootstrapPhase(data.bootstrapPhase())
                .treasury(data.treasury())
                .lastEnactedGovActionIds(data.lastEnactedGovActionIds())
                .build();
    }

    private CommitteeMember mapToCommitteeMember(CommitteeMemberDetails committeeMemberDetails) {
        return CommitteeMember.builder()
                .coldKey(committeeMemberDetails.getColdKey())
                .hotKey(committeeMemberDetails.getHotKey())
                .startEpoch(committeeMemberDetails.getStartEpoch())
                .expiredEpoch(committeeMemberDetails.getExpiredEpoch())
                .credType(committeeMemberDetails.getCredType())
                .build();
    }
}
