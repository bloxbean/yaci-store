package com.bloxbean.cardano.yaci.store.governanceaggr.util;

import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DRepExpiryUtil {

    /**
     * Calculates the expiry epoch for a DRep in epoch boundary based on registration and activity history.
     *
     * <p>This method determines when a DRep's registration should expire, according to
     * Conway-era governance rules. The expiry calculation takes into account:</p>
     *
     * <ul>
     *   <li><b>Epoch of last relevant activity</b> — either the last interaction (vote/update) or registration epoch if no interaction has occurred.</li>
     *   <li><b>Activity window (dRepActivity)</b> — the number of active epochs granted after each interaction or registration.</li>
     *   <li><b>Number of dormant epochs</b> — epochs with no active proposals, counted from the last activity to the current epoch.</li>
     *   <li><b>Protocol version bonus</b> — for protocol version 9, an additional bonus period may be granted.</li>
     * </ul>
     *
     * <p>The final expiry epoch is computed as:</p>
     * <pre>
     *     expiry = baseEpoch + dRepActivity + dormantCount [+ v9Bonus]
     * </pre>
     * Where <code>baseEpoch</code> is the last interaction epoch or registration epoch.
     *
     * @param registrationInfo          Information about the DRep registration, including epoch, slot, and drepActivity.
     * @param lastDRepInteraction       Information about the last DRep interaction (vote or update), or {@code null} if none occurred.
     * @param dormantEpochs             A set of dormant epochs (epochs with no active proposals).
     * @param proposalsUpToRegistration List of all proposal submissions from the start of Conway era up to the DRep's registration epoch.
     * @param eraFirstEpoch             The first epoch of the Conway era.
     * @param evaluatedEpoch            The epoch that just ended
     * @return the expiry epoch.
     */
    public static int calculateDRepExpiry(
            DRepRegistrationInfo registrationInfo,
            @Nullable DRepInteractionInfo lastDRepInteraction,
            Set<Integer> dormantEpochs,
            List<ProposalSubmissionInfo> proposalsUpToRegistration,
            int eraFirstEpoch,
            int evaluatedEpoch
    ) {
        int result;
        boolean hasPostRegistrationInteraction = lastDRepInteraction != null;

        // Choose the epoch to be used as the base for expiry calculation
        int lastActivityEpoch = hasPostRegistrationInteraction
                ? lastDRepInteraction.epoch()
                : registrationInfo.epoch();

        int dormantCount = (int) dormantEpochs.stream()
                .filter(e -> e > lastActivityEpoch && e <= evaluatedEpoch)
                .count();

        int activityWindow = lastDRepInteraction != null ? lastDRepInteraction.dRepActivity() : registrationInfo.dRepActivity();

        // Calculate the base expiry without applying protocol v9 bonus
        int baseExpiry = lastActivityEpoch + activityWindow + dormantCount;
        int v9bonus = 0;

        // For registration with protocol version >= 10, or if the DRep has interacted after registration, no v9 bonus is applied
        if (registrationInfo.protocolMajorVersion() >= 10 || lastDRepInteraction != null) {
            result = baseExpiry;
        } else {
            // For protocol version 9 and no post-registration interaction, calculate the bonus expiry window
            v9bonus = computeV9Bonus(registrationInfo, proposalsUpToRegistration, eraFirstEpoch);
            result = baseExpiry + v9bonus;
        }

        if (result <= evaluatedEpoch) {
            // the DRep is being inactive, dormant epochs do not affect the expiry value when a DRep is in an inactive state.
            result = calculateInactiveDRepExpiry(lastActivityEpoch, activityWindow, v9bonus, evaluatedEpoch, dormantEpochs);
        }

        return result;
    }

    /**
     * Computes the additional bonus epochs added to the expiry calculation
     * for DReps registered under protocol version 9.
     *
     * <p>This bonus only applies if:</p>
     * <ul>
     *   <li>The DRep has not performed any interaction (e.g., vote or update) after registration.</li>
     *   <li>A dormant period (no active proposals) can be identified before or during registration.</li>
     * </ul>
     *
     * <p>The bonus is determined based on the registration timing:</p>
     * <ul>
     *   <li>If the DRep registered before any proposal was submitted, the bonus equals {@code registeredEpoch - eraFirstEpoch + 1}.</li>
     *   <li>If the DRep registered during or before the end of a dormant period, the bonus equals the length of that dormant period.</li>
     * </ul>
     *
     * @param registrationInfo          The DRep registration info.
     * @param proposalsUpToRegistration All proposals up to DRep registration epoch.
     * @param eraFirstEpoch             First epoch of the Conway era.
     * @return Bonus in number of epochs, or 0 if no bonus applies.
     */
    public static int computeV9Bonus(
            DRepRegistrationInfo registrationInfo,
            List<ProposalSubmissionInfo> proposalsUpToRegistration,
            int eraFirstEpoch) {

        int registeredEpoch = registrationInfo.epoch();
        long registeredSlot = registrationInfo.slot();

        // Case 1: No proposals at all → grant bonus equal to all epochs since start of Conway era
        if (proposalsUpToRegistration.isEmpty()) {
            return registeredEpoch - eraFirstEpoch + 1;
        }

        ProposalSubmissionInfo firstProposal = proposalsUpToRegistration.stream()
                .min(Comparator.comparingInt(ProposalSubmissionInfo::epoch)
                        .thenComparingLong(ProposalSubmissionInfo::slot))
                .get();

        // Case 2: Registered before the first proposal → grant full bonus
        if (registeredEpoch < firstProposal.epoch()
                || (registeredEpoch == firstProposal.epoch()
                && registeredSlot <= firstProposal.slot())) {
            return registeredEpoch - eraFirstEpoch + 1;
        }

        // Otherwise, try to find a dormant period prior to or overlapping registration
        List<ProposalSubmissionInfo> sortedProposals = proposalsUpToRegistration.stream()
                .sorted(Comparator
                        .comparingInt(ProposalSubmissionInfo::epoch)
                        .thenComparingLong(ProposalSubmissionInfo::slot)
                        .reversed())
                .toList();

        // Try to find the last dormant period before or overlapping registration
        DormantPeriod dormantPeriod = findLastDormantPeriod(sortedProposals, registeredEpoch);

        // If no dormant period found → no bonus applies
        if (dormantPeriod == null) return 0;

        // If the dormant period has no defined ending slot → it's considered ongoing at the time of registration
        if (dormantPeriod.endingSlot().isEmpty()) {
            return dormantPeriod.length();
        }

        // Otherwise, only apply the bonus if the DRep registered before or at the end of the dormant period
        return registeredSlot <= dormantPeriod.endingSlot().get() ? dormantPeriod.length() : 0;
    }

    /**
     * Finds the most recent dormant period that occurred before or overlapped
     * with the DRep's registration epoch.
     *
     * @param proposals List of proposals
     * @param registrationEpoch   The epoch of registration.
     * @return The last dormant period or {@code null} if none.
     */
    private static DormantPeriod findLastDormantPeriod(List<ProposalSubmissionInfo> proposals, int registrationEpoch) {
        int epoch = registrationEpoch;
        Long slot = null;

        for (ProposalSubmissionInfo proposal : proposals) {
            // If the gap between proposal expiry and current epoch > 0 → dormant period detected
            int gap = epoch - proposal.epoch() - proposal.govActionLifeTime();
            if (gap > 0) {
                if (slot == null) {
                    return new DormantPeriod(Optional.empty(), gap);
                } else {
                    return new DormantPeriod(Optional.of(slot), gap);
                }
            }

            epoch = proposal.epoch();
            slot = proposal.slot();
        }

        return null;
    }

    /**
     * Recalculates the expiry epoch for a DRep that has become inactive.
     *
     * When a DRep becomes inactive (expiry value at epoch X is less than epoch X), dormant epochs
     * no longer affect the expiry calculation. This method determines the exact epoch
     * just before the DRep became inactive, equivalent to the expiry value before the epoch became inactive
     *
     * @param lastActionEpoch The epoch of the last DRep action (registration, update, or vote)
     * @param activityWindow The number of active epochs granted after each interaction
     * @param v9Bonus Additional epochs granted for protocol version 9 (if applicable)
     * @param evaluatedEpoch The epoch that just ended
     * @param dormantEpochs Set of epochs considered dormant (no active proposals)
     * @return The exact epoch when the DRep's activity period ended
     */
    private static int calculateInactiveDRepExpiry(
            int lastActionEpoch,
            int activityWindow,
            int v9Bonus,
            int evaluatedEpoch,
            Set<Integer> dormantEpochs) {

        /*
        dRep expiry = lastActionEpoch + dormant epoch count + (activityWindow + v9Bonus(if applicable));
        with each dormant epoch, the expiry value is increased by 1.
        Starting from (the last action epoch + 1),
        if the number of non-dormant epochs exceeds (activityWindow + v9Bonus), then the drep becomes inactive.
        */
        int nonDormantEpochCount = 0;

        for (int _epoch = lastActionEpoch + 1; _epoch <= evaluatedEpoch; _epoch++) {
            if (!dormantEpochs.contains(_epoch)) {
                nonDormantEpochCount++;
            }

            if (nonDormantEpochCount == activityWindow + v9Bonus) {
                return _epoch;
            }
        }

        return evaluatedEpoch;
    }

    /**
     * Checks whether all epochs in a given range are dormant.
     *
     * @param fromEpoch     The starting epoch (inclusive).
     * @param toEpoch       The ending epoch (inclusive).
     * @param dormantEpochs A set of epochs considered dormant (no active proposals).
     * @return {@code true} if all epochs in the range are dormant; {@code false} otherwise.
     */
    public static boolean isEpochRangeDormant(int fromEpoch, int toEpoch, Set<Integer> dormantEpochs) {
        for (int i = fromEpoch; i <= toEpoch; i++) {
            if (!dormantEpochs.contains(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Contains metadata about a DRep's registration on the Cardano blockchain.
     *
     * @param slot                  The slot at which the registration transaction was included.
     * @param epoch                 The epoch corresponding to the registration slot.
     * @param dRepActivity          The {@code dRepActivity} value from protocol parameters at the time of registration.
     * @param protocolMajorVersion  The major protocol version at the time of registration.
     */
    public record DRepRegistrationInfo(long slot, int epoch, int dRepActivity, int protocolMajorVersion) {
    }

    /**
     * Represents the interaction performed by a DRep
     * (voting or update).
     *
     * @param epoch         The epoch in which the interaction occurred.
     * @param dRepActivity  The {@code dRepActivity} value from protocol parameters at the time of registration.
     */
    public record DRepInteractionInfo(int epoch, int dRepActivity) {
    }
    /**
     * Represents a governance proposal submission
     *
     * @param slot               The slot at which the proposal was submitted.
     * @param epoch              The epoch in which the proposal was submitted.
     * @param govActionLifeTime  The {@code govActionLifeTime} value from protocol parameters, the number of epochs the proposal is considered active.
     */
    public record ProposalSubmissionInfo(long slot, int epoch, int govActionLifeTime) {
    }

    private record DormantPeriod(Optional<Long> endingSlot, int length) {
    }
}
