package com.bloxbean.cardano.yaci.store.governancerules.rule;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ProtocolParamGroup;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProtocolParamUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class DRepVotingState extends VotingState {
    private BigInteger yesVoteStake;
    private BigInteger noVoteStake;
    private DrepVoteThresholds dRepVotingThresholds;
    private ConstitutionCommitteeState ccState;

    @Override
    public boolean isAccepted() {
        if (ccState == null || dRepVotingThresholds == null || yesVoteStake == null || noVoteStake == null) {
            return false;
        }

        BigInteger totalExcludingAbstainStake = yesVoteStake.add(noVoteStake);
        BigDecimal acceptedStakeRatio;
        if (totalExcludingAbstainStake.equals(BigInteger.ZERO)) {
            acceptedStakeRatio = BigDecimal.ZERO;
        } else {
            acceptedStakeRatio = BigNumberUtils.divide(yesVoteStake, totalExcludingAbstainStake);
        }

        final GovActionType govActionType = govAction.getType();
        boolean result = false;

        switch (govActionType) {
            case PARAMETER_CHANGE_ACTION:
                result = isAcceptedForParameterChangeAction(acceptedStakeRatio);
                break;
            case TREASURY_WITHDRAWALS_ACTION:
                result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtTreasuryWithdrawal()));
                break;
            case HARD_FORK_INITIATION_ACTION:
                result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtHardForkInitiation()));
                break;
            case NO_CONFIDENCE:
                result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtMotionNoConfidence()));
                break;
            case NEW_CONSTITUTION:
                result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtUpdateToConstitution()));
                break;
            case UPDATE_COMMITTEE:
                if (ccState == ConstitutionCommitteeState.NORMAL) {
                    result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtCommitteeNormal()));
                } else {
                    result = BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, safeRatio(dRepVotingThresholds.getDvtCommitteeNoConfidence()));
                }

                break;
        }

        return result;
    }

    // Since an individual update can contain multiple groups, the actual thresholds are then
    // given by taking the maximum of all those thresholds
    private boolean isAcceptedForParameterChangeAction(BigDecimal acceptedStakeRatio) {
        List<ProtocolParamGroup> ppGroupChangeList = ProtocolParamUtil.getGroupsWithNonNullField(
                ((ParameterChangeAction) govAction).getProtocolParamUpdate());

        BigDecimal maxThreshold = BigDecimal.ZERO;

        for (var ppGroup : ppGroupChangeList) {
            maxThreshold = maxThreshold.max(getThresholdForParamGroup(ppGroup));
        }

        return BigNumberUtils.isHigherOrEquals(acceptedStakeRatio, maxThreshold);
    }

    private BigDecimal getThresholdForParamGroup(ProtocolParamGroup ppGroup) {
        return switch (ppGroup) {
            case NETWORK -> safeRatio(dRepVotingThresholds.getDvtPPNetworkGroup());
            case ECONOMIC -> safeRatio(dRepVotingThresholds.getDvtPPEconomicGroup());
            case GOVERNANCE -> safeRatio(dRepVotingThresholds.getDvtPPGovGroup());
            case TECHNICAL -> safeRatio(dRepVotingThresholds.getDvtPPTechnicalGroup());
            default -> throw new IllegalArgumentException("Unsupported protocol parameter group: " + ppGroup);
        };
    }

}
