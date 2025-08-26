package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.HardForkInitiationAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.CommitteeVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.SPOVotingState;

/**
 * Evaluator for evaluating Hard Fork Initiation governance actions.
 */
public class HardForkRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);

        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }

        HardForkInitiationAction hardForkInitiationAction = (HardForkInitiationAction) context.getGovAction();
        SPOVotingState spoVotingState = buildSPOVotingState(context);
        CommitteeVotingState committeeVotingState = buildCommitteeVotingState(context);
        DRepVotingState dRepVotingState = buildDRepVotingState(context);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.HARD_FORK);

        final boolean isAccepted = context.isBootstrapPhase() ?
                committeeVotingState.isAccepted() && spoVotingState.isAccepted()
                : committeeVotingState.isAccepted() && dRepVotingState.isAccepted() && spoVotingState.isAccepted();

        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(hardForkInitiationAction.getType(), hardForkInitiationAction.getGovActionId(), lastEnactedGovActionId);

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
            throw new IllegalArgumentException("Committee votes are required for Hard Fork Initiation actions");
        }

        if (!context.getVotingData().hasDRepVotes()) {
            throw new IllegalArgumentException("DRep votes are required for Hard Fork Initiation actions");
        }

        if (!context.getVotingData().hasSPOVotes()) {
            throw new IllegalArgumentException("SPO votes are required for Hard Fork Initiation actions");
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
