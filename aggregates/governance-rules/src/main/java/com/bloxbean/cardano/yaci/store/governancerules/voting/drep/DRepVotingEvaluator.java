package com.bloxbean.cardano.yaci.store.governancerules.voting.drep;

import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VoteTallyCalculator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

public class DRepVotingEvaluator implements VotingEvaluator<VotingData> {

    @Override
    public VotingStatus evaluate(VotingData votingData, VotingEvaluationContext context) {
        var drepData = votingData.getDrepVotes();
        if (drepData == null || context.getDrepThresholds() == null) {
            return VotingStatus.INSUFFICIENT_DATA;
        }

        var dRepVoteTallies = VoteTallyCalculator.computeDRepTallies(drepData, context.getGovAction().getType());
        BigInteger totalYes = dRepVoteTallies.getTotalYesStake();
        BigInteger totalNo = dRepVoteTallies.getTotalNoStake();
        BigInteger totalYesAndNo = totalYes.add(totalNo);
        
        if (totalYesAndNo.equals(BigInteger.ZERO)) {
            return VotingStatus.PASS_THRESHOLD;
        }
        // the ratio = yes/(yes + no)
        BigDecimal acceptedRatio = BigNumberUtils.divide(totalYes, totalYesAndNo);

        BigDecimal requiredThreshold = getRequiredThreshold(context);
        
        return BigNumberUtils.isHigherOrEquals(acceptedRatio, requiredThreshold) ?
            VotingStatus.PASS_THRESHOLD : VotingStatus.NOT_PASS_THRESHOLD;
    }

    private BigDecimal getRequiredThreshold(VotingEvaluationContext context) {
        var thresholds = context.getDrepThresholds();
        
        return switch (context.getGovAction().getType()) {
            case PARAMETER_CHANGE_ACTION -> getParameterChangeThreshold(context);
            case TREASURY_WITHDRAWALS_ACTION -> safeRatio(thresholds.getDvtTreasuryWithdrawal());
            case HARD_FORK_INITIATION_ACTION -> safeRatio(thresholds.getDvtHardForkInitiation());
            case NO_CONFIDENCE -> safeRatio(thresholds.getDvtMotionNoConfidence());
            case NEW_CONSTITUTION -> safeRatio(thresholds.getDvtUpdateToConstitution());
            case UPDATE_COMMITTEE -> context.getCommittee().getState() == ConstitutionCommitteeState.NORMAL ?
                safeRatio(thresholds.getDvtCommitteeNormal()) :
                safeRatio(thresholds.getDvtCommitteeNoConfidence());
            default -> throw new IllegalArgumentException("Unsupported action type: " + context.getGovAction().getType());
        };
    }

    // Since an individual update can contain multiple groups, the actual thresholds are then
    // given by taking the maximum of all those thresholds
    private BigDecimal getParameterChangeThreshold(VotingEvaluationContext context) {
        var paramUpdate = ((ParameterChangeAction) context.getGovAction()).getProtocolParamUpdate();
        List<ProtocolParamGroup> groups = ProtocolParamUtil.getGroupsWithNonNullField(paramUpdate);
        
        return groups.stream()
            .map(group -> getThresholdForGroup(group, context))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal getThresholdForGroup(ProtocolParamGroup group, VotingEvaluationContext context) {
        var thresholds = context.getDrepThresholds();
        return switch (group) {
            case NETWORK -> safeRatio(thresholds.getDvtPPNetworkGroup());
            case ECONOMIC -> safeRatio(thresholds.getDvtPPEconomicGroup());
            case GOVERNANCE -> safeRatio(thresholds.getDvtPPGovGroup());
            case TECHNICAL -> safeRatio(thresholds.getDvtPPTechnicalGroup());
            default -> throw new IllegalArgumentException("Unsupported group: " + group);
        };
    }
}
