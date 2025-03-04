package com.bloxbean.cardano.yaci.store.governancerules.rule;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.governancerules.util.NumericUtil.toDouble;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Slf4j
public class SPOVotingState extends VotingState {
    private BigInteger yesVoteStake;
    private BigInteger abstainVoteStake;
    private BigInteger totalStake;
    private ConstitutionCommitteeState ccState;
    private PoolVotingThresholds poolVotingThresholds;

    @Override
    public boolean isAccepted() {
        boolean result;
        double acceptedStakeRatio;

        if (poolVotingThresholds == null || yesVoteStake == null || abstainVoteStake == null || totalStake == null || ccState == null) {
            return false;
        }

        if (totalStake.equals(BigInteger.ZERO) || abstainVoteStake.equals(totalStake)) {
            acceptedStakeRatio = 0;
        } else {
            acceptedStakeRatio = toDouble(yesVoteStake) / toDouble(totalStake.add(abstainVoteStake.negate()));
        }

        final GovActionType govActionType = govAction.getType();

        switch (govActionType) {
            case NO_CONFIDENCE:
                result = acceptedStakeRatio >= toDouble(poolVotingThresholds.getPvtMotionNoConfidence());
                break;
            case UPDATE_COMMITTEE:
                if (ccState == ConstitutionCommitteeState.NORMAL) {
                    result = acceptedStakeRatio >= toDouble(poolVotingThresholds.getPvtCommitteeNormal());
                } else {
                    result = acceptedStakeRatio >= toDouble(poolVotingThresholds.getPvtCommitteeNoConfidence());
                }
                break;
            case HARD_FORK_INITIATION_ACTION:
                result = acceptedStakeRatio >= toDouble(poolVotingThresholds.getPvtHardForkInitiation());
                break;
            case PARAMETER_CHANGE_ACTION:
                // security group
                result = yesVoteStake.doubleValue() >= toDouble(poolVotingThresholds.getPvtPPSecurityGroup());
                break;
            default:
                log.error("SPOs do not vote this action");
                result = false;
        }

        return result;
    }

}
