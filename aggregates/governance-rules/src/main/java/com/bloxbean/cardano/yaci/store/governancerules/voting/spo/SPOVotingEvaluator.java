package com.bloxbean.cardano.yaci.store.governancerules.voting.spo;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

public class SPOVotingEvaluator implements VotingEvaluator<VotingData> {
    
    @Override
    public VotingStatus evaluate(VotingData votingData, VotingEvaluationContext context) {
        var spoData = votingData.getSpoVotes();
        if (spoData == null || context.getPoolThresholds() == null) {
            return VotingStatus.INSUFFICIENT_DATA;
        }
        
        GovActionType actionType = context.getGovAction().getType();
        if (!isSPOVotingRequired(actionType)) {
            return VotingStatus.INSUFFICIENT_DATA;
        }

        var spoVoteTallies = VoteTallyCalculator.computeSPOTallies(spoData, actionType, context.isInBootstrapPhase());
        BigInteger totalYes = spoVoteTallies.getTotalYesStake();
        BigInteger totalAbstain = spoVoteTallies.getTotalAbstainStake();
        BigInteger totalStake = spoData.getTotalStake();
        
        if (totalStake.equals(BigInteger.ZERO) || totalAbstain.equals(totalStake)) {
            return VotingStatus.PASS_THRESHOLD;
        }

        // the ratio = yes/(total - abstain)
        BigDecimal acceptedRatio = new BigDecimal(totalYes)
            .divide(new BigDecimal(totalStake.subtract(totalAbstain)), BigNumberUtils.mathContext);
            
        BigDecimal requiredThreshold = getRequiredThreshold(actionType, context);
        
        return BigNumberUtils.isHigherOrEquals(acceptedRatio, requiredThreshold) ?
                VotingStatus.PASS_THRESHOLD : VotingStatus.NOT_PASS_THRESHOLD;
    }
    
    private boolean isSPOVotingRequired(GovActionType actionType) {
        return actionType == GovActionType.NO_CONFIDENCE ||
               actionType == GovActionType.UPDATE_COMMITTEE ||
               actionType == GovActionType.HARD_FORK_INITIATION_ACTION ||
               actionType == GovActionType.PARAMETER_CHANGE_ACTION;
    }

    private BigDecimal getRequiredThreshold(GovActionType actionType, VotingEvaluationContext context) {
        var thresholds = context.getPoolThresholds();
        
        return switch (actionType) {
            case NO_CONFIDENCE -> safeRatio(thresholds.getPvtMotionNoConfidence());
            case UPDATE_COMMITTEE -> context.getCommittee().getState() == ConstitutionCommitteeState.NORMAL ?
                safeRatio(thresholds.getPvtCommitteeNormal()) :
                safeRatio(thresholds.getPvtCommitteeNoConfidence());
            case HARD_FORK_INITIATION_ACTION -> safeRatio(thresholds.getPvtHardForkInitiation());
            case PARAMETER_CHANGE_ACTION -> safeRatio(thresholds.getPvtPPSecurityGroup());
            default -> throw new IllegalArgumentException("SPOs do not vote on: " + actionType);
        };
    }
}