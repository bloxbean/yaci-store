package com.bloxbean.cardano.yaci.store.governancerules.util;


import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

public class GovernanceActionUtil {

    private GovernanceActionUtil() {
    }

    /**
     * A proposal can only be enacted if the previously enacted proposal of the same kind has the GovActionID mentioned in the to be enacted proposal.
     * This method check that the previous governance action id specified in the proposal
     * does match the last one of the same purpose that was enacted
     *
     * @param govActionType
     * @param prevGovActionID
     * @param enactedGovActionID
     * @return
     */
    public static boolean verifyPrevGovAction(GovActionType govActionType,
                                              GovActionId prevGovActionID,
                                              GovActionId enactedGovActionID) {
        if (govActionType == GovActionType.TREASURY_WITHDRAWALS_ACTION || govActionType == GovActionType.INFO_ACTION) {
            return true;
        }

        return Objects.equals(enactedGovActionID.getGov_action_index(), prevGovActionID.getGov_action_index())
                && Objects.equals(enactedGovActionID.getTransactionId(), prevGovActionID.getTransactionId());
    }


    public static boolean isValidCommitteeTerm(UpdateCommittee updateCommittee, Integer committeeMaxTermLength) {
        if (committeeMaxTermLength == null) {
            return true;
        }
        final Map<Credential, Integer> newMembersAndTerms = updateCommittee.getNewMembersAndTerms();
        if (newMembersAndTerms != null) {
            var memberWithInvalidTerm = newMembersAndTerms.values().stream().filter(term -> term != null && term > committeeMaxTermLength).findFirst();
            return memberWithInvalidTerm.isEmpty();
        }

        return false;
    }

    public static boolean withdrawalCanWithdraw(TreasuryWithdrawalsAction treasuryWithdrawalsAction, BigInteger treasury) {
        final Map<String, BigInteger> withdrawals = treasuryWithdrawalsAction.getWithdrawals();
        if (withdrawals == null) {
            return true;
        }

        BigInteger totalWithdrawal = BigInteger.ZERO;
        for (BigInteger withdrawalAmount : withdrawals.values()) {
            totalWithdrawal = totalWithdrawal.add(withdrawalAmount);
        }

        return totalWithdrawal.compareTo(treasury) <= 0;
    }

    public static boolean isExpired(int expiredEpoch, int currentEpoch) {
        return expiredEpoch < currentEpoch;
    }

    public static boolean isDelayingAction(GovActionType govActionType) {
        return govActionType == GovActionType.NO_CONFIDENCE || govActionType == GovActionType.NEW_CONSTITUTION
            || govActionType == GovActionType.UPDATE_COMMITTEE || govActionType == GovActionType.HARD_FORK_INITIATION_ACTION;
    }
}
