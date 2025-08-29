package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.HardForkInitiationAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;
import com.bloxbean.cardano.yaci.store.governancerules.voting.committee.CommitteeVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;

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

        VotingEvaluationContext votingEvaluationContext = buildVotingEvaluationContext(context);

        VotingResult committeeVotingResult = new CommitteeVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
        VotingResult spoVotingResult = new SPOVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
        VotingResult dRepVotingResult = new DRepVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.HARD_FORK);

        final boolean isAccepted = context.isBootstrapPhase() ?
                committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD)
                        && spoVotingResult.equals(VotingResult.PASSED_THRESHOLD)
                :
                committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD)
                        && spoVotingResult.equals(VotingResult.PASSED_THRESHOLD)
                        && dRepVotingResult.equals(VotingResult.PASSED_THRESHOLD);

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

        if (context.getVotingData().getCommitteeVotes() == null) {
            throw new IllegalArgumentException("Committee votes are required for Hard Fork Initiation actions");
        }

        if (context.getVotingData().getDrepVotes() == null) {
            throw new IllegalArgumentException("DRep votes are required for Hard Fork Initiation actions");
        }

        if (context.getVotingData().getSpoVotes() == null) {
            throw new IllegalArgumentException("SPO votes are required for Hard Fork Initiation actions");
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
