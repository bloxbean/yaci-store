package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.CommitteeVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;

/**
 * Evaluator for evaluating Treasury Withdrawal governance actions.
 */
public class TreasuryWithdrawalRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);
        
        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }
        
        TreasuryWithdrawalsAction treasuryAction = (TreasuryWithdrawalsAction) context.getGovAction();
        
        // Check if withdrawal amount is valid
        boolean withdrawalCanWithdraw = GovernanceActionUtil.withdrawalCanWithdraw(
            treasuryAction, 
            context.getGovernanceContext().getTreasury()
        );
        
        if (!withdrawalCanWithdraw) {
            return RatificationResult.REJECT;
        }
        
        CommitteeVotingState committeeVotingState = buildCommitteeVotingState(context);
        DRepVotingState drepVotingState = buildDRepVotingState(context);
        
        boolean isAccepted = committeeVotingState.isAccepted() && drepVotingState.isAccepted();
        boolean isNotDelayed = context.isNotDelayed() && context.isCommitteeNormal();
        
        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
        }
    }
    
    @Override
    public void validateRequiredData(RatificationContext context) {
        RatificationEvaluator.super.validateRequiredData(context);
        
        if (!context.getVotingData().hasCommitteeVotes()) {
            throw new IllegalArgumentException("Committee votes are required for Treasury Withdrawal actions");
        }
        
        if (!context.getVotingData().hasDRepVotes()) {
            throw new IllegalArgumentException("DRep votes are required for Treasury Withdrawal actions");
        }
        
        if (context.getGovernanceContext().getTreasury() == null) {
            throw new IllegalArgumentException("Treasury amount is required for Treasury Withdrawal actions");
        }
    }
    
    private CommitteeVotingState buildCommitteeVotingState(RatificationContext context) {

        return CommitteeVotingState.builder()
                .govAction(context.getGovAction())
                .committee(context.getGovernanceContext().getCommittee())
                .votes(context.getVotingData().getCommitteeVotes().getVotes())
                .build();
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
}
