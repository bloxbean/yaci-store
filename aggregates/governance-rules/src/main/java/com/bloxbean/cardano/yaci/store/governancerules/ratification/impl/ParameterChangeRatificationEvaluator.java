package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;
import com.bloxbean.cardano.yaci.store.governancerules.voting.committee.CommitteeVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;

import java.util.List;

/**
 * Evaluator for evaluating Parameter Change governance actions.
 */
public class ParameterChangeRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        validateRequiredData(context);

        if (context.isProposalExpired()) {
            return RatificationResult.REJECT;
        }

        ParameterChangeAction parameterChangeAction = (ParameterChangeAction) context.getGovAction();
        VotingEvaluationContext votingEvaluationContext = buildVotingEvaluationContext(context);

        VotingResult committeeVotingResult = new CommitteeVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.P_PARAM_UPDATE);
        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(parameterChangeAction.getType(), parameterChangeAction.getGovActionId(), lastEnactedGovActionId);

        boolean isAccepted;

        if (context.isBootstrapPhase()) {
            isAccepted = committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD);
        } else {
            List<ProtocolParamGroup> ppGroupChangeList = ProtocolParamUtil.getGroupsWithNonNullField(parameterChangeAction.getProtocolParamUpdate());
            VotingResult dRepVotingResult = new DRepVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);

            if (ppGroupChangeList.contains(ProtocolParamGroup.SECURITY)) {
                VotingResult spoVotingResult = new SPOVotingEvaluator().evaluate(context.getVotingData(), votingEvaluationContext);
                if (ppGroupChangeList.size() == 1) {
                    isAccepted = committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD) && spoVotingResult.equals(VotingResult.PASSED_THRESHOLD);
                } else
                    isAccepted = committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD) && spoVotingResult.equals(VotingResult.PASSED_THRESHOLD) && dRepVotingResult.equals(VotingResult.PASSED_THRESHOLD);
            } else
                isAccepted = committeeVotingResult.equals(VotingResult.PASSED_THRESHOLD) && dRepVotingResult.equals(VotingResult.PASSED_THRESHOLD);
        }

        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
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
