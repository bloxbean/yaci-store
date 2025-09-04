package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationInput;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalContext;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProposalStateService {
    private final ProposalStateClient proposalStateClient;
    private final EpochParamStorage epochParamStorage;
    private final CommitteeStorage committeeStorage;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final AdaPotStorage adaPotStorage;
    private final BootstrapPhaseDetector bootstrapPhaseDetector;
    private final ProposalCollectionService proposalCollectionService;
    private final VotingDataCollector votingDataService;

    public GovernanceEvaluationInput collectGovernanceData(int currentEpoch) {
        if (log.isDebugEnabled()) {
            log.debug("Collecting governance data for epoch: {}", currentEpoch);
        }

        final boolean isInConwayBootstrapPhase = bootstrapPhaseDetector.isInConwayBootstrapPhase(currentEpoch);
        
        List<GovActionProposal> proposalsForEvaluation = proposalCollectionService.getProposalsForStatusEvaluation(currentEpoch);

        if (proposalsForEvaluation.isEmpty()) {
            return null;
        }

        Map<GovActionId, VotingData> votingDataMap = votingDataService.collectVotingDataBatch(proposalsForEvaluation, currentEpoch - 1);
        
        List<ProposalContext> currentProposalContexts = proposalCollectionService.createProposalContexts(proposalsForEvaluation, votingDataMap);

        // Get protocol parameters
        var epochParam = epochParamStorage.getProtocolParams(currentEpoch)
                .orElseThrow(() -> new IllegalStateException("Protocol params not found for epoch: " + currentEpoch));

        // Get committee
        var committee = committeeStorage.getCommitteeByEpoch(currentEpoch)
                .orElseThrow(() -> new IllegalStateException("Committee not found for epoch: " + (currentEpoch)));
        List<CommitteeMemberDetails> membersCanVote = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(currentEpoch - 1);

        ConstitutionCommittee constitutionCommittee = ConstitutionCommittee.builder()
                .members(membersCanVote.stream().map(this::mapToCommitteeMember).toList())
                .threshold(new UnitInterval(committee.getThresholdNumerator(), committee.getThresholdDenominator()))
                .build();

        // Get treasury
        var treasury = adaPotStorage.findByEpoch(currentEpoch)
                .map(AdaPot::getTreasury)
                .orElseThrow(() -> new IllegalStateException("Treasury not found for epoch: " + currentEpoch));

        // Get last enacted gov actions
        Map<ProposalType, GovActionId> lastEnactedActions = getLastEnactedGovActions(currentEpoch);

        return GovernanceEvaluationInput.builder()
                .currentProposals(currentProposalContexts)
                .currentEpoch(currentEpoch)
                .protocolParams(epochParam.getParams())
                .committee(constitutionCommittee)
                .isBootstrapPhase(isInConwayBootstrapPhase)
                .treasury(treasury)
                .lastEnactedGovActionIds(lastEnactedActions)
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

    private Map<ProposalType, GovActionId> getLastEnactedGovActions(int currentEpoch) {
        Map<ProposalType, GovActionId> lastEnactedActions = new HashMap<>();

        GovActionId lastEnactedHardForkGovActionId = proposalStateClient.getLastEnactedProposal(GovActionType.HARD_FORK_INITIATION_ACTION, currentEpoch)
                .map(this::fromGovActionProposal)
                .orElse(null);
        lastEnactedActions.put(ProposalType.HARD_FORK, lastEnactedHardForkGovActionId);

        GovActionId lastEnactedParamChangeGovActionId = proposalStateClient.getLastEnactedProposal(GovActionType.PARAMETER_CHANGE_ACTION, currentEpoch)
                .map(this::fromGovActionProposal)
                .orElse(null);
        lastEnactedActions.put(ProposalType.P_PARAM_UPDATE, lastEnactedParamChangeGovActionId);

        GovActionId lastEnactedNewConstitutionGovActionId = proposalStateClient.getLastEnactedProposal(GovActionType.NEW_CONSTITUTION, currentEpoch)
                .map(this::fromGovActionProposal)
                .orElse(null);
        lastEnactedActions.put(ProposalType.CONSTITUTION, lastEnactedNewConstitutionGovActionId);

        var lastEnactedNewCommitteeGovAction = proposalStateClient.getLastEnactedProposal(GovActionType.UPDATE_COMMITTEE, currentEpoch);
        var lastEnactedNoConfidenceGovAction = proposalStateClient.getLastEnactedProposal(GovActionType.NO_CONFIDENCE, currentEpoch);

        if (lastEnactedNewCommitteeGovAction.isPresent() && lastEnactedNoConfidenceGovAction.isPresent()) {
            if (lastEnactedNewCommitteeGovAction.get().getEpoch() > lastEnactedNoConfidenceGovAction.get().getEpoch()) {
                lastEnactedActions.put(ProposalType.COMMITTEE, fromGovActionProposal(lastEnactedNewCommitteeGovAction.get()));
            } else {
                lastEnactedActions.put(ProposalType.COMMITTEE, fromGovActionProposal(lastEnactedNoConfidenceGovAction.get()));
            }
        } else if (lastEnactedNewCommitteeGovAction.isPresent()) {
            lastEnactedActions.put(ProposalType.COMMITTEE, fromGovActionProposal(lastEnactedNewCommitteeGovAction.get()));
        } else if (lastEnactedNoConfidenceGovAction.isPresent()) {
            lastEnactedActions.put(ProposalType.COMMITTEE, fromGovActionProposal(lastEnactedNoConfidenceGovAction.get()));
        } else {
            lastEnactedActions.put(ProposalType.COMMITTEE, null);
        }

        return lastEnactedActions;
    }

    private GovActionId fromGovActionProposal(GovActionProposal proposal) {
        return GovActionId.builder()
                .transactionId(proposal.getTxHash())
                .gov_action_index(proposal.getIndex())
                .build();
    }

}
