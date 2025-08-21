package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.rule.DRepVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.rule.SPOVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;

/**
 * Evaluator for evaluating No Confidence governance actions.
 */
public class NoConfidenceRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);
        
        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }
        
        NoConfidence noConfidence = (NoConfidence) context.getGovAction();
        
        DRepVotingState drepVotingState = buildDRepVotingState(context);
        SPOVotingState spoVotingState = buildSPOVotingState(context);
        
        boolean isAccepted = drepVotingState.isAccepted() && spoVotingState.isAccepted();
        
        boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(
                    noConfidence.getType(),
                    noConfidence.getGovActionId(),
                    context.getProposalContext().getLastEnactedGovActionId());
        
        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
        }
    }
    
    @Override
    public void validateRequiredData(RatificationContext context) {
        RatificationEvaluator.super.validateRequiredData(context);
        
        if (!context.getVotingData().hasDRepVotes()) {
            throw new IllegalArgumentException("DRep votes are required for No Confidence actions");
        }
        
        if (!context.getVotingData().hasSPOVotes()) {
            throw new IllegalArgumentException("SPO votes are required for No Confidence actions");
        }
    }
    
    private DRepVotingState buildDRepVotingState(RatificationContext context) {
        return DRepVotingState.builder()
                .govAction(context.getGovAction())
                .dRepVotingThresholds(context.getGovernanceState().getEpochParam().getParams().getDrepVotingThresholds())
                .yesVoteStake(context.getVotingData().getDrepVotes().getYesVoteStake())
                .noVoteStake(context.getVotingData().getDrepVotes().getNoVoteStake())
                .ccState(context.getGovernanceState().getCommitteeState())
                .build();
    }
    
    private SPOVotingState buildSPOVotingState(RatificationContext context) {
        return SPOVotingState.builder()
                .govAction(context.getGovAction())
                .poolVotingThresholds(context.getGovernanceState().getEpochParam().getParams().getPoolVotingThresholds())
                .yesVoteStake(context.getVotingData().getSpoVotes().getYesVoteStake())
                .abstainVoteStake(context.getVotingData().getSpoVotes().getAbstainVoteStake())
                .totalStake(context.getVotingData().getSpoVotes().getTotalStake())
                .ccState(context.getGovernanceState().getCommitteeState())
                .build();
    }
}
