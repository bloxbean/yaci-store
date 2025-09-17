package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import com.bloxbean.cardano.yaci.store.governancerules.voting.committee.CommitteeVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;

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

        VotingEvaluationContext votingEvaluationContext = buildVotingEvaluationContext(context);

        VotingStatus committeeVotingResult = new CommitteeVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
        VotingStatus dRepVotingResult = new DRepVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

        boolean isAccepted = committeeVotingResult.equals(VotingStatus.PASS_THRESHOLD) && dRepVotingResult.equals(VotingStatus.PASS_THRESHOLD);
        boolean isNotDelayed = context.isNotDelayed() && context.isCommitteeNormal();
        
        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed && withdrawalCanWithdraw) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed && withdrawalCanWithdraw) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
        }
    }
    
    @Override
    public void validateRequiredData(RatificationContext context) {
        RatificationEvaluator.super.validateRequiredData(context);
        
        if (context.getVotingData().getCommitteeVotes() == null) {
            throw new IllegalArgumentException("Committee votes are required for Treasury Withdrawal actions");
        }
        
        if (context.getVotingData().getDrepVotes() == null) {
            throw new IllegalArgumentException("DRep votes are required for Treasury Withdrawal actions");
        }
        
        if (context.getGovernanceContext().getTreasury() == null) {
            throw new IllegalArgumentException("Treasury amount is required for Treasury Withdrawal actions");
        }
    }

    private VotingEvaluationContext buildVotingEvaluationContext(RatificationContext context) {
        return VotingEvaluationContext.builder()
                .govAction(context.getGovAction())
                .committee(context.getGovernanceContext().getCommittee())
                .drepThresholds(context.getGovernanceContext().getProtocolParams().getDrepVotingThresholds())
                .poolThresholds(context.getGovernanceContext().getProtocolParams().getPoolVotingThresholds())
                .build();
    }
}
