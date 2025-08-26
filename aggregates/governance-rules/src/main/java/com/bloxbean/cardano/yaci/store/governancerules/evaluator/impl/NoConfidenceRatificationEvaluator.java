package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.SPOVotingState;
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
        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.COMMITTEE);

        final boolean isAccepted = drepVotingState.isAccepted() && spoVotingState.isAccepted();

        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(
                    noConfidence.getType(),
                    noConfidence.getGovActionId(),
                    lastEnactedGovActionId);
        
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
                .dRepVotingThresholds(context.getGovernanceContext().getProtocolParams().getDrepVotingThresholds())
                .yesVoteStake(context.getVotingData().getDrepVotes().getYesVoteStake())
                .noVoteStake(context.getVotingData().getDrepVotes().getNoVoteStake())
                .doNotVoteStake(context.getVotingData().getDrepVotes().getDoNotVoteStake())
                .noConfidenceStake(context.getVotingData().getDrepVotes().getNoConfidenceStake())
                .ccState(context.getGovernanceContext().getCommittee().getState())
                .build();
    }
    
    private SPOVotingState buildSPOVotingState(RatificationContext context) {
        var govContext = context.getGovernanceContext();

        return SPOVotingState.builder()
                .govAction(context.getGovAction())
                .poolVotingThresholds(govContext.getProtocolParams().getPoolVotingThresholds())
                .yesVoteStake(context.getVotingData().getSpoVotes().getYesVoteStake())
                .abstainVoteStake(context.getVotingData().getSpoVotes().getAbstainVoteStake())
                .totalStake(context.getVotingData().getSpoVotes().getTotalStake())
                .ccState(govContext.getCommittee().getState())
                .build();
    }
}
