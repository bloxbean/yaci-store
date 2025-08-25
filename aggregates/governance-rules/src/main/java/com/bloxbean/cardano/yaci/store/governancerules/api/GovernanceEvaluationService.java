package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.GovernanceContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluatorFactory;
import com.bloxbean.cardano.yaci.store.governancerules.service.ProposalDropService;

import java.util.ArrayList;
import java.util.List;


public class GovernanceEvaluationService {
    
    private final ProposalDropService proposalDropService;

    public GovernanceEvaluationService() {
        this.proposalDropService = new ProposalDropService();
    }

    public GovernanceEvaluationResult evaluateGovernanceState(GovernanceEvaluationInput input) {
        input.validate();
        
        List<ProposalEvaluationResult> proposalResults = evaluateIndividualProposals(input);

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

        // TODO: check if action ratification is delayed
        boolean isActionRatificationDelayed = false;

        return GovernanceEvaluationResult.builder()
            .proposalResults(proposalResults)
            .proposalsToDropNext(droppedProposals)
            .isActionRatificationDelayed(isActionRatificationDelayed)
            .build();
    }

    private List<ProposalEvaluationResult> evaluateIndividualProposals(GovernanceEvaluationInput input) {
        
        List<ProposalEvaluationResult> results = new ArrayList<>();
        
        for (ProposalContext proposalContext : input.getCurrentProposals()) {

            GovernanceContext governanceContext = GovernanceContext.builder()
                .currentEpoch(input.getCurrentEpoch())
                .protocolParams(input.getProtocolParams())
                .committee(input.getCommittee())
                .isBootstrapPhase(input.isBootstrapPhase())
                .treasury(input.getTreasury())
                .lastEnactedGovActionIds(input.getLastEnactedGovActionIds())
                .build();

            // Build ratification context
            RatificationContext context = RatificationContext.builder()
                .govAction(proposalContext.getGovAction())
                .votingData(proposalContext.getVotingData())
                .governanceContext(governanceContext)
                .build();
            
            // Get appropriate evaluator and evaluate
            RatificationResult result = RatificationEvaluatorFactory
                .getEvaluator(proposalContext.getGovAction().getType())
                .evaluate(context);
            
            // Build detailed result
            ProposalEvaluationResult evaluationResult = ProposalEvaluationResult.builder()
                    .proposal(Proposal.builder().type(proposalContext.getGovAction().getType())
                            .govActionId(proposalContext.getGovActionId())
                            .prevGovActionId(proposalContext.getPreviousGovActionId())
                            .build())
                .status(result)
                .build();
            
            results.add(evaluationResult);
        }
        
        return results;
    }

}
