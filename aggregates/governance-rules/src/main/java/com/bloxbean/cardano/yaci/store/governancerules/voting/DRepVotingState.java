package com.bloxbean.cardano.yaci.store.governancerules.voting;

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
    private DrepVoteThresholds dRepVotingThresholds;
    private ConstitutionCommitteeState ccState;

    BigInteger yesVoteStake;
    BigInteger noConfidenceStake;
    BigInteger noVoteStake;
    BigInteger doNotVoteStake;

    @Override
    public boolean isAccepted() {
        BigInteger totalYesStake = getTotalYesStake();
        BigInteger totalNoStake = getTotalNoStake();

        BigInteger totalExcludingAbstainStake = totalYesStake.add(totalNoStake);

        BigDecimal acceptedStakeRatio;
        if (totalExcludingAbstainStake.equals(BigInteger.ZERO)) {
            acceptedStakeRatio = BigDecimal.ZERO;
        } else {
            acceptedStakeRatio = BigNumberUtils.divide(totalYesStake, totalExcludingAbstainStake);
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

    public BigInteger getTotalYesStake() {
        /*
            Total DRep yes stake – The total stake of:
            1. Registered dReps that voted 'Yes', plus
            2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
         */
        BigInteger totalYesStake = yesVoteStake;

        if (govAction.getType().equals(GovActionType.NO_CONFIDENCE) && noConfidenceStake != null) {
            totalYesStake = totalYesStake.add(noConfidenceStake);
        }

        return totalYesStake;
    }

    public BigInteger getTotalNoStake() {
       /*
        DRep No Stake – The total stake of:
        1. Registered dReps that voted 'No', plus
        2. Registered dReps that did not vote for this action, plus
        3. The AlwaysNoConfidence dRep.
       */
        BigInteger totalNoStake = noVoteStake;
        if (noConfidenceStake != null) {
            totalNoStake = totalNoStake.add(noConfidenceStake);
        }
        if (doNotVoteStake != null) {
            totalNoStake = totalNoStake.add(doNotVoteStake);
        }
        return totalNoStake;
    }

}
