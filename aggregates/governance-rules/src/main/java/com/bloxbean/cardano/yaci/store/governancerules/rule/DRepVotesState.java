package com.bloxbean.cardano.yaci.store.governancerules.rule;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.governancerules.util.NumericUtil.toDouble;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class DRepVotesState extends VotesState {
    private BigInteger yesVoteStake;
    private BigInteger noVoteStake;
    private DrepVoteThresholds dRepVotingThresholds;
    private ConstitutionCommitteeState ccState;

    @Override
    public boolean isAccepted() {
        final double acceptedStakeRatio = toDouble(yesVoteStake) / toDouble(yesVoteStake.add(noVoteStake));
        final GovActionType govActionType = govAction.getType();
        boolean result;

        switch (govActionType) {
            case PARAMETER_CHANGE_ACTION:
                result = isAcceptedForParameterChangeAction(acceptedStakeRatio);
                break;
            case TREASURY_WITHDRAWALS_ACTION:
                result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtTreasuryWithdrawal());
                break;
            case HARD_FORK_INITIATION_ACTION:
                result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtHardForkInitiation());
                break;
            case NO_CONFIDENCE:
                result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtMotionNoConfidence());
                break;
            case NEW_CONSTITUTION:
                result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtUpdateToConstitution());
                break;
            case INFO_ACTION:
                result = acceptedStakeRatio == 1;
                break;
            case UPDATE_COMMITTEE:
                if (ccState == ConstitutionCommitteeState.NORMAL) {
                    result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtCommitteeNormal());
                } else {
                    result = acceptedStakeRatio >= toDouble(dRepVotingThresholds.getDvtCommitteeNoConfidence());
                }

                break;
            default:
                throw new RuntimeException("SPOs do not vote this action");
        }

        return result;
    }

    // Since an individual update can contain multiple groups, the actual thresholds are then
    // given by taking the maximum of all those thresholds
    private boolean isAcceptedForParameterChangeAction(double acceptedStakeRatio) {
        List<ProtocolParamGroup> ppGroupChangeList = ProtocolParamUtil.getGroupsWithNonNullField(
                ((ParameterChangeAction) govAction).getProtocolParamUpdate());

        double maxThreshold = 0;

        for (var ppGroup : ppGroupChangeList) {
            maxThreshold = Math.max(maxThreshold, getThresholdForParamGroup(ppGroup));
        }

        return acceptedStakeRatio >= maxThreshold;
    }

    private double getThresholdForParamGroup(ProtocolParamGroup ppGroup) {
        return switch (ppGroup) {
            case NETWORK -> toDouble(dRepVotingThresholds.getDvtPPNetworkGroup());
            case ECONOMIC -> toDouble(dRepVotingThresholds.getDvtPPEconomicGroup());
            case GOVERNANCE -> toDouble(dRepVotingThresholds.getDvtPPGovGroup());
            case TECHNICAL -> toDouble(dRepVotingThresholds.getDvtPPTechnicalGroup());
            default -> throw new IllegalArgumentException("Unsupported protocol parameter group: " + ppGroup);
        };
    }

}
