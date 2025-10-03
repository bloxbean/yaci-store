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
     * @param lastEnactedGovActionID
     * @return
     */
    public static boolean isPrevActionAsExpected(GovActionType govActionType,
                                                 GovActionId prevGovActionID,
                                                 GovActionId lastEnactedGovActionID) {
        if (govActionType == GovActionType.TREASURY_WITHDRAWALS_ACTION || govActionType == GovActionType.INFO_ACTION
                || lastEnactedGovActionID == null) {
            return true;
        }

        if (prevGovActionID == null) {
            return false;
        }

        return Objects.equals(lastEnactedGovActionID.getGov_action_index(), prevGovActionID.getGov_action_index())
                && Objects.equals(lastEnactedGovActionID.getTransactionId(), prevGovActionID.getTransactionId());
    }

    /**
     * Checks if the committee term specified in a governance action is valid.
     *
     * @param updateCommittee The update committee action
     * @param currentEpoch The current epoch
     * @param committeeMaxTermLength The maximum term length allowed for a committee member
     * @return {@code true} if the expiration of all new committee members is less than or equal to the maximum term.
     *         {@code false} otherwise.
     */
    public static boolean isValidCommitteeTerm(UpdateCommittee updateCommittee, Integer committeeMaxTermLength, Integer currentEpoch) {
        if (committeeMaxTermLength == null) {
            return true;
        }
        final Map<Credential, Integer> newMembersAndTerms = updateCommittee.getNewMembersAndTerms();
        if (newMembersAndTerms != null) {
            return newMembersAndTerms.values().stream().allMatch(term -> term != null && term <= currentEpoch + committeeMaxTermLength);
        }

        return false;
    }

    /**
     * Checks if the withdrawal can be processed based on the given governance action and treasury amount.
     *
     * @param treasuryWithdrawalsAction The treasury withdrawals action
     * @param treasury The current treasury balance
     * @return {@code true} if the sum of all withdrawals does not exceed the treasury amount,
     *         {@code false} otherwise.
     */
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


    // todo: comment here
    public static boolean isDelayingAction(GovActionType govActionType) {
        return govActionType == GovActionType.HARD_FORK_INITIATION_ACTION
                || govActionType == GovActionType.NEW_CONSTITUTION
                || govActionType == GovActionType.UPDATE_COMMITTEE
                || govActionType == GovActionType.NO_CONFIDENCE;
    }

    // todo: add link to ledger code
    public static int getActionPriority(GovActionType govActionType) {
        return switch (govActionType) {
            case NO_CONFIDENCE -> 0;
            case UPDATE_COMMITTEE -> 1;
            case NEW_CONSTITUTION -> 2;
            case HARD_FORK_INITIATION_ACTION -> 3;
            case PARAMETER_CHANGE_ACTION -> 4;
            case TREASURY_WITHDRAWALS_ACTION -> 5;
            case INFO_ACTION -> 6;
        };
    }
}
