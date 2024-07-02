package com.bloxbean.cardano.yaci.store.governancerules.rule;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.Objects;

import static com.bloxbean.cardano.yaci.store.governancerules.util.NumericUtil.toDouble;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class SPOVotingState extends VotingState {
    private BigInteger yesVoteStake;
    private BigInteger abstainVoteStake;
    private BigInteger totalStake;
    private ConstitutionCommitteeState ccState;
    private PoolVotingThresholds poolVotingThresholds;

    @Override
    public boolean isAccepted() {
        boolean result;
        final double acceptedStakeRatio = toDouble(yesVoteStake) / toDouble(totalStake.add(abstainVoteStake.negate()));
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
            case INFO_ACTION:
                result = Objects.equals(yesVoteStake, abstainVoteStake);
                break;
            case PARAMETER_CHANGE_ACTION:
                // security group
                result = yesVoteStake.doubleValue() >= toDouble(poolVotingThresholds.getPvtPPSecurityGroup());
                break;
            default:
                throw new RuntimeException("SPOs do not vote this action");
        }

        return result;
    }

}
