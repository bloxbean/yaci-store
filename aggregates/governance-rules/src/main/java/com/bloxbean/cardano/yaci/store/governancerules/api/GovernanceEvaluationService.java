package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.GovernanceContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluatorFactory;
import com.bloxbean.cardano.yaci.store.governancerules.service.ProposalDropService;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class GovernanceEvaluationService {

    private final ProposalDropService proposalDropService;

    public GovernanceEvaluationService() {
        this.proposalDropService = new ProposalDropService();
    }

    public GovernanceEvaluationResult evaluateGovernanceState(GovernanceEvaluationInput input) {
        input.validate();

        List<ProposalEvaluationResult> proposalResults = evaluateIndividualProposals(input);

        boolean isActionRatificationDelayed = proposalResults.stream()
                .anyMatch(result ->
                        result.getStatus() == RatificationResult.ACCEPT &&
                                GovernanceActionUtil.isDelayingAction(result.getProposal().getType())
                );

        List<Proposal> expiredProposals = proposalResults.stream()
                .filter(result -> result.getStatus() == RatificationResult.REJECT)
                .map(ProposalEvaluationResult::getProposal)
                .toList();

        List<Proposal> ratifiedProposals = proposalResults.stream()
                .filter(result -> result.getStatus() == RatificationResult.ACCEPT)
                .map(ProposalEvaluationResult::getProposal)
                .toList();

        List<Proposal> droppedProposals
                = proposalDropService.getProposalsBeDropped(
                input.getCurrentProposals().stream().map(proposalContext ->
                                Proposal.builder().govActionId(proposalContext.getGovActionId())
                                        .prevGovActionId(proposalContext.getPreviousGovActionId())
                                        .type(proposalContext.getGovAction().getType())
                                        .build())
                        .toList(),
                expiredProposals, ratifiedProposals);

        return GovernanceEvaluationResult.builder()
                .proposalResults(proposalResults)
                .proposalsToDropNext(droppedProposals)
                .isActionRatificationDelayed(isActionRatificationDelayed)
                .build();
    }

    private List<ProposalEvaluationResult> evaluateIndividualProposals(GovernanceEvaluationInput input) {
        List<ProposalEvaluationResult> results = new ArrayList<>();

        List<ProposalContext> sortedProposals = input.getCurrentProposals().stream()
                .sorted(Comparator.comparingInt((ProposalContext proposal) -> GovernanceActionUtil.getActionPriority(proposal.getGovAction().getType()))
                        .thenComparingLong(ProposalContext::getProposalSlot))
                .toList();

        boolean isDelayedByDelayingAction = false;

        for (ProposalContext proposalContext : sortedProposals) {
            RatificationResult result = evaluateProposal(proposalContext, input, isDelayedByDelayingAction);

            ProposalEvaluationResult evaluationResult = ProposalEvaluationResult.builder()
                    .proposal(Proposal.builder().type(proposalContext.getGovAction().getType())
                            .govActionId(proposalContext.getGovActionId())
                            .prevGovActionId(proposalContext.getPreviousGovActionId())
                            .build())
                    .status(result)
                    .build();

            results.add(evaluationResult);

            if (GovernanceActionUtil.isDelayingAction(proposalContext.getGovAction().getType()) && result == RatificationResult.ACCEPT) {
                isDelayedByDelayingAction = true;
            }
        }

        return results;
    }

    private RatificationResult evaluateProposal(
            ProposalContext proposalContext,
            GovernanceEvaluationInput input,
            boolean isDelayedByDelayingAction) {

        GovernanceContext governanceContext = GovernanceContext.builder()
                .currentEpoch(input.getCurrentEpoch())
                .protocolParams(input.getProtocolParams())
                .committee(input.getCommittee())
                .isInBootstrapPhase(input.isBootstrapPhase())
                .treasury(input.getTreasury())
                .lastEnactedGovActionIds(input.getLastEnactedGovActionIds())
                .isActionRatificationDelayed(isDelayedByDelayingAction)
                .build();

        RatificationContext context = RatificationContext.builder()
                .govAction(proposalContext.getGovAction())
                .votingData(proposalContext.getVotingData())
                .governanceContext(governanceContext)
                .build();

        return RatificationEvaluatorFactory
                .getEvaluator(proposalContext.getGovAction().getType())
                .evaluate(context);
    }

}
