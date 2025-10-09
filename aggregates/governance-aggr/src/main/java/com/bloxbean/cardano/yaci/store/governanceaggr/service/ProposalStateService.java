package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedGovernanceData;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
/*
    Collects all data required for governance evaluation in an epoch
 */
public class ProposalStateService {
    private final ProposalStateClient proposalStateClient;
    private final EpochParamStorage epochParamStorage;
    private final CommitteeStorage committeeStorage;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final AdaPotStorage adaPotStorage;
    private final BootstrapPhaseService bootstrapPhaseDetector;
    private final ProposalCollectionService proposalCollectionService;
    private final CommitteeStateService committeeStateService;
    private final VotingDataCollector votingDataService;

    public AggregatedGovernanceData collectGovernanceData(int epoch) {
        if (log.isDebugEnabled()) {
            log.debug("Collecting governance data for epoch: {}", epoch);
        }

        final boolean isInConwayBootstrapPhase = bootstrapPhaseDetector.isInConwayBootstrapPhase(epoch);

        // Get proposals for status evaluation
        List<GovActionProposal> proposalsForEvaluation = proposalCollectionService.getProposalsForStatusEvaluation(epoch);

        if (proposalsForEvaluation.isEmpty()) {
            return null;
        }

        Map<GovActionId, AggregatedVotingData> votingDataMap = votingDataService.collectVotingDataBatch(proposalsForEvaluation, epoch - 1);

        // Get protocol parameters
        var epochParam = epochParamStorage.getProtocolParams(epoch)
                .orElseThrow(() -> new IllegalStateException("Protocol params not found for epoch: " + epoch));

        // Get committee
        var committee = committeeStorage.getCommitteeByEpoch(epoch)
                .orElseThrow(() -> new IllegalStateException("Committee not found for epoch: " + (epoch)));
        List<CommitteeMemberDetails> membersCanVote = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(epoch - 1);
        var committeeState = committeeStateService.getCurrentCommitteeState();

        // Get treasury
        var treasury = adaPotStorage.findByEpoch(epoch)
                .map(AdaPot::getTreasury)
                .orElseThrow(() -> new IllegalStateException("Treasury not found for epoch: " + epoch));

        // Get last enacted gov actions
        Map<ProposalType, GovActionId> lastEnactedActions = getLastEnactedGovActions(epoch);

        return AggregatedGovernanceData.builder()
                .epoch(epoch)
                .proposalsForEvaluation(proposalsForEvaluation)
                .aggregatedVotingDataByProposal(votingDataMap)
                .protocolParams(epochParam.getParams())
                .committeeThresholdNumerator(committee.getThresholdNumerator())
                .committeeThresholdDenominator(committee.getThresholdDenominator())
                .committeeMembers(membersCanVote)
                .committeeState(committeeState)
                .treasury(treasury)
                .lastEnactedGovActionIds(lastEnactedActions)
                .bootstrapPhase(isInConwayBootstrapPhase)
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
