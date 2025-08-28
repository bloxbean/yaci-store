package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;

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

        VotingEvaluationContext votingEvaluationContext = buildVotingEvaluationContext(context);
        VotingResult spoVotingResult = new SPOVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
        VotingResult dRepVotingResult = new DRepVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

        final boolean isAccepted = dRepVotingResult.equals(VotingResult.PASSED_THRESHOLD) && spoVotingResult.equals(VotingResult.PASSED_THRESHOLD);

        final boolean isValidCommitteeTerm = GovernanceActionUtil.isValidCommitteeTerm(updateCommittee,
                context.getGovernanceContext().getProtocolParams().getCommitteeMaxTermLength(), context.getGovernanceContext().getCurrentEpoch());

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

    private VotingEvaluationContext buildVotingEvaluationContext(RatificationContext context) {
        return VotingEvaluationContext.builder()
                .govAction(context.getGovAction())
                .committee(context.getGovernanceContext().getCommittee())
                .drepThresholds(context.getGovernanceContext().getProtocolParams().getDrepVotingThresholds())
                .poolThresholds(context.getGovernanceContext().getProtocolParams().getPoolVotingThresholds())
                .build();
    }
}
