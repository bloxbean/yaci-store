package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.SPOVotingState;

/**
 * Evaluator for evaluating Update Committee governance actions.
 */
public class UpdateCommitteeRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);

        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }

        UpdateCommittee updateCommittee = (UpdateCommittee) context.getGovAction();

        final boolean isValidCommitteeTerm = GovernanceActionUtil.isValidCommitteeTerm(updateCommittee,
                context.getGovernanceContext().getProtocolParams().getCommitteeMaxTermLength(), context.getGovernanceContext().getCurrentEpoch());

        DRepVotingState dRepVotingState = buildDRepVotingState(context);
        SPOVotingState spoVotingState = buildSPOVotingState(context);
        final boolean isAccepted = dRepVotingState.isAccepted() && spoVotingState.isAccepted();
        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.COMMITTEE);

        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(updateCommittee.getType(), updateCommittee.getGovActionId(), lastEnactedGovActionId);

        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed && isValidCommitteeTerm) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed && isValidCommitteeTerm) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
        }
    }

    @Override
    public void validateRequiredData(RatificationContext context) {
        RatificationEvaluator.super.validateRequiredData(context);

        if (!context.getVotingData().hasSPOVotes()) {
            throw new IllegalArgumentException("SPO votes are required for Update Committee actions");
        }

        if (!context.getVotingData().hasDRepVotes()) {
            throw new IllegalArgumentException("DRep votes are required for Update Committee actions");
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
