package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.CommitteeVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.DRepVotingState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.SPOVotingState;

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
        CommitteeVotingState committeeVotingState = buildCommitteeVotingState(context);

        GovActionId lastEnactedGovActionId = context.getGovernanceContext().getLastEnactedGovActionIds().get(ProposalType.P_PARAM_UPDATE);
        final boolean isNotDelayed = context.isNotDelayed()
                && context.isCommitteeNormal()
                && GovernanceActionUtil.isPrevActionAsExpected(parameterChangeAction.getType(), parameterChangeAction.getGovActionId(), lastEnactedGovActionId);

        boolean isAccepted;

        if (context.isBootstrapPhase()) {
            isAccepted = committeeVotingState.isAccepted();
        } else {
            List<ProtocolParamGroup> ppGroupChangeList = ProtocolParamUtil.getGroupsWithNonNullField(parameterChangeAction.getProtocolParamUpdate());
            DRepVotingState dRepVotingState = buildDRepVotingState(context);

            if (ppGroupChangeList.contains(ProtocolParamGroup.SECURITY)) {
                SPOVotingState spoVotingState = buildSPOVotingState(context);
                if (ppGroupChangeList.size() == 1) {
                    isAccepted = committeeVotingState.isAccepted() && spoVotingState.isAccepted();
                } else
                    isAccepted = committeeVotingState.isAccepted() && spoVotingState.isAccepted() && dRepVotingState.isAccepted();
            } else
                isAccepted = committeeVotingState.isAccepted() && dRepVotingState.isAccepted();
        }

        if (context.isLastVotingEpoch()) {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.REJECT;
        } else {
            return (isAccepted && isNotDelayed) ? RatificationResult.ACCEPT : RatificationResult.CONTINUE;
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
