package com.bloxbean.cardano.yaci.store.governancerules.voting.spo;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

public class SPOVotingEvaluator implements VotingEvaluator<VotingData> {
    
    @Override
    public VotingResult evaluate(VotingData votingData, VotingEvaluationContext context) {
        var spoData = votingData.getSpoVotes();
        if (spoData == null || context.getPoolThresholds() == null) {
            return VotingResult.INSUFFICIENT_DATA;
        }
        
        GovActionType actionType = context.getGovAction().getType();
        if (!isSPOVotingRequired(actionType)) {
            return VotingResult.INSUFFICIENT_DATA;
        }
        
        BigInteger totalYes = calculateTotalYesStake(spoData, actionType);
        BigInteger totalAbstain = calculateTotalAbstainStake(spoData, actionType, context.isInBootstrapPhase());
        BigInteger totalStake = spoData.getTotalStake();
        
        if (totalStake.equals(BigInteger.ZERO) || totalAbstain.equals(totalStake)) {
            return VotingResult.PASSED_THRESHOLD;
        }
        
        BigInteger activeStake = totalStake.subtract(totalAbstain);
        BigDecimal acceptedRatio = new BigDecimal(totalYes)
            .divide(new BigDecimal(activeStake), BigNumberUtils.mathContext);
            
        BigDecimal requiredThreshold = getRequiredThreshold(actionType, context);
        
        return BigNumberUtils.isHigherOrEquals(acceptedRatio, requiredThreshold) ?
            VotingResult.PASSED_THRESHOLD : VotingResult.NOT_PASSED_THRESHOLD;
    }
    
    private boolean isSPOVotingRequired(GovActionType actionType) {
        return actionType == GovActionType.NO_CONFIDENCE ||
               actionType == GovActionType.UPDATE_COMMITTEE ||
               actionType == GovActionType.HARD_FORK_INITIATION_ACTION ||
               actionType == GovActionType.PARAMETER_CHANGE_ACTION;
    }
    
    private BigInteger calculateTotalYesStake(VotingData.SPOVotes data, GovActionType actionType) {
        BigInteger total = data.getYesVoteStake();
        if (actionType == GovActionType.NO_CONFIDENCE && data.getDelegateToNoConfidenceDRepStake() != null) {
            total = total.add(data.getDelegateToNoConfidenceDRepStake());
        }
        return total;
    }
    
    private BigInteger calculateTotalAbstainStake(VotingData.SPOVotes data, GovActionType actionType, boolean isBootstrap) {
        BigInteger total = data.getAbstainVoteStake();
        if (data.getDelegateToAutoAbstainDRepStake() != null) {
            total = total.add(data.getDelegateToAutoAbstainDRepStake());
        }
        if (isBootstrap && actionType != GovActionType.HARD_FORK_INITIATION_ACTION && data.getDoNotVoteStake() != null) {
            total = total.add(data.getDoNotVoteStake());
        }
        return total;
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