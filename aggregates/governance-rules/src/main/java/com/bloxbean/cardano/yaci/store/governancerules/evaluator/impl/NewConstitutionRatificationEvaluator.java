package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.CommitteeVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;

/**
 * Evaluator for evaluating New Constitution governance actions.
 */
public class NewConstitutionRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);

        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }

        NewConstitution newConstitution = (NewConstitution) context.getGovAction();
        DRepVotingState dRepVotingState = buildDRepVotingState(context);
        CommitteeVotingState committeeVotingState = buildCommitteeVotingState(context);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.CONSTITUTION);

        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(newConstitution.getType(), newConstitution.getGovActionId(), lastEnactedGovActionId);

        final boolean isAccepted = committeeVotingState.isAccepted() && dRepVotingState.isAccepted();

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
            throw new IllegalArgumentException("Committee votes are required for New Constitution actions");
        }

        if (!context.getVotingData().hasDRepVotes()) {
            throw new IllegalArgumentException("DRep votes are required for New Constitution actions");
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
