package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.mapper.ProtocolParamUpdateMapper;
import com.bloxbean.cardano.yaci.store.governancerules.domain.GovernanceContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluatorFactory;
import com.bloxbean.cardano.yaci.store.governancerules.service.ProposalDropService;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;

/*
 * Service to evaluate the governance state for a given epoch.
 * It evaluates each proposal and determines which proposals should be dropped.
 */
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
                        .thenComparingLong(ProposalContext::getProposalSlot)
                        .thenComparingInt(ProposalContext::getTxIndex)
                        .thenComparingInt(proposal -> proposal.getGovActionId().getGov_action_index()))
                .toList();

        // ENACT changes the state used by later actions, without changing the caller's snapshot.
        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.merge(input.getProtocolParams());
        GovernanceContext governanceContext = GovernanceContext.builder()
                .currentEpoch(input.getCurrentEpoch())
                .protocolParams(protocolParams)
                .committee(input.getCommittee())
                .isInBootstrapPhase(input.isBootstrapPhase())
                .treasury(input.getTreasury())
                .lastEnactedGovActionIds(input.getLastEnactedGovActionIds() == null
                        ? new HashMap<>() : new HashMap<>(input.getLastEnactedGovActionIds()))
                .build();

        for (ProposalContext proposalContext : sortedProposals) {
            RatificationResult result = evaluateProposal(proposalContext, governanceContext);

            ProposalEvaluationResult evaluationResult = ProposalEvaluationResult.builder()
                    .proposal(Proposal.builder().type(proposalContext.getGovAction().getType())
                            .govActionId(proposalContext.getGovActionId())
                            .prevGovActionId(proposalContext.getPreviousGovActionId())
                            .build())
                    .status(result)
                    .build();

            results.add(evaluationResult);

            if (result == RatificationResult.ACCEPT) {
                if (GovernanceActionUtil.isDelayingAction(proposalContext.getGovAction().getType())) {
                    // No later action can ratify, so changes to committee/constitution/version
                    // cannot affect this pass. Only expiry is checked for the remaining proposals.
                    governanceContext = governanceContext.toBuilder().isActionRatificationDelayed(true).build();
                } else {
                    governanceContext = enactNonDelayingAction(proposalContext, governanceContext);
                }
            }
        }

        return results;
    }

    private GovernanceContext enactNonDelayingAction(ProposalContext proposal, GovernanceContext state) {
        return switch (proposal.getGovAction().getType()) {
            case PARAMETER_CHANGE_ACTION -> {
                var action = (ParameterChangeAction) proposal.getGovAction();
                state.getProtocolParams().merge(ProtocolParamUpdateMapper.INSTANCE.toProtocolParams(action.getProtocolParamUpdate()));
                state.getLastEnactedGovActionIds().put(ProposalType.P_PARAM_UPDATE, proposal.getGovActionId());
                yield state;
            }
            case TREASURY_WITHDRAWALS_ACTION -> {
                var action = (TreasuryWithdrawalsAction) proposal.getGovAction();
                BigInteger withdrawals = action.getWithdrawals().values().stream()
                        .reduce(BigInteger.ZERO, BigInteger::add);
                yield state.toBuilder().treasury(state.getTreasury().subtract(withdrawals)).build();
            }
            default -> state;
        };
    }

    private RatificationResult evaluateProposal(ProposalContext proposalContext, GovernanceContext governanceContext) {
        if (governanceContext.isActionRatificationDelayed()) {
            return governanceContext.getCurrentEpoch() > proposalContext.getMaxAllowedVotingEpoch()
                    ? RatificationResult.REJECT : RatificationResult.CONTINUE;
        }

        RatificationContext context = RatificationContext.builder()
                .govAction(proposalContext.getGovAction())
                .votingData(proposalContext.getVotingData())
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(proposalContext.getMaxAllowedVotingEpoch())
                .build();

        return RatificationEvaluatorFactory
                .getEvaluator(proposalContext.getGovAction().getType())
                .evaluate(context);
    }

}
