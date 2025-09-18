package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;

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

        VotingEvaluationContext votingEvaluationContext = buildVotingEvaluationContext(context);
        VotingStatus spoVotingResult = new SPOVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
        VotingStatus dRepVotingResult = new DRepVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.COMMITTEE);

        final boolean isAccepted = dRepVotingResult.equals(VotingStatus.PASS_THRESHOLD) && spoVotingResult.equals(VotingStatus.PASS_THRESHOLD);

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
        
        if (context.getVotingData().getDrepVotes() == null) {
            throw new IllegalArgumentException("DRep votes are required for No Confidence actions");
        }
        
        if (context.getVotingData().getSpoVotes() == null) {
            throw new IllegalArgumentException("SPO votes are required for No Confidence actions");
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
