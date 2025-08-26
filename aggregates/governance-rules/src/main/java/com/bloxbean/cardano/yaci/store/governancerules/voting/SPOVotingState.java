package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils.isHigherOrEquals;
import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Slf4j
public class SPOVotingState extends VotingState {
    private BigInteger yesVoteStake;
    private BigInteger delegateToAutoAbstainDRepStake;
    private BigInteger delegateToNoConfidenceDRepStake;
    private BigInteger abstainVoteStake;
    private BigInteger doNotVoteStake;
    private BigInteger totalStake;

    private ConstitutionCommitteeState ccState;
    private PoolVotingThresholds poolVotingThresholds;
    private boolean isInBoostrapPhase;

    @Override
    public boolean isAccepted() {
        boolean result;
        BigDecimal acceptedStakeRatio;

        BigInteger totalYesStake = getTotalYesStake();
        BigInteger totalAbstainStake = getTotalAbstainStake();

        if (totalStake.equals(BigInteger.ZERO) || totalAbstainStake.equals(totalStake)) {
            acceptedStakeRatio = BigDecimal.ZERO;
        } else {
            BigInteger totalStakeMinusAbstainStake = totalStake.add(totalAbstainStake.negate());
            acceptedStakeRatio = new BigDecimal(totalYesStake).divide(new BigDecimal(totalStakeMinusAbstainStake), BigNumberUtils.mathContext);
        }

        final GovActionType govActionType = govAction.getType();

        switch (govActionType) {
            case NO_CONFIDENCE:
                result = isHigherOrEquals(acceptedStakeRatio, safeRatio(poolVotingThresholds.getPvtMotionNoConfidence()));
                break;
            case UPDATE_COMMITTEE:
                if (ccState == ConstitutionCommitteeState.NORMAL) {//
                    result = isHigherOrEquals(acceptedStakeRatio, safeRatio(poolVotingThresholds.getPvtCommitteeNormal()));
                } else {
                    result = isHigherOrEquals(acceptedStakeRatio, safeRatio(poolVotingThresholds.getPvtCommitteeNoConfidence()));
                }
                break;
            case HARD_FORK_INITIATION_ACTION:
                result = isHigherOrEquals(acceptedStakeRatio, safeRatio(poolVotingThresholds.getPvtHardForkInitiation()));
                break;
            case PARAMETER_CHANGE_ACTION:
                // security group
                result = isHigherOrEquals(acceptedStakeRatio, safeRatio(poolVotingThresholds.getPvtPPSecurityGroup()));
                break;
            default:
                log.error("SPOs do not vote this action");
                result = false;
        }

        return result;
    }


    public BigInteger getTotalYesStake() {

        BigInteger totalYesStake = yesVoteStake;
        if (govAction.getType() == GovActionType.NO_CONFIDENCE) {
            totalYesStake = totalYesStake.add(delegateToNoConfidenceDRepStake);
        }
        return totalYesStake;
    }

    public BigInteger getTotalAbstainStake() {
        BigInteger totalAbstainStake = abstainVoteStake.add(delegateToAutoAbstainDRepStake);
        if (isInBoostrapPhase && govAction.getType() != GovActionType.HARD_FORK_INITIATION_ACTION) {
            totalAbstainStake = totalAbstainStake.add(doNotVoteStake);
        }
        return totalAbstainStake;
    }
}
