package com.bloxbean.cardano.yaci.store.governanceaggr.util;

import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param registrationInfo               Information about the DRep registration, including epoch, slot, and drepActivity.
     * @param lastDRepInteraction            Information about the last DRep interaction (vote or update), or {@code null} if none occurred.
     * @param dormantEpochs                  A set of dormant epochs (epochs with no active proposals).
     * @param latestProposalUpToRegistration Information of the most recent proposal submitted up to and including the DRep registration slot.
     * @param eraFirstEpoch                  The first epoch of the Conway era.
     * @param evaluatedEpoch                 The epoch that just ended
     * @return the expiry epoch.
     */
    public static int calculateDRepExpiry(
            DRepRegistrationInfo registrationInfo,
            @Nullable DRepInteractionInfo lastDRepInteraction,
            Set<Integer> dormantEpochs,
            @Nullable ProposalSubmissionInfo latestProposalUpToRegistration,
            int eraFirstEpoch,
            int evaluatedEpoch
    ) {
        if (registrationInfo.protocolMajorVersion() >= 10) {
            return calculateDRepExpiryV10(
                    registrationInfo,
                    lastDRepInteraction,
                    dormantEpochs,
                    evaluatedEpoch
            );
        } else {
            return calculateDRepExpiryV9(
                    registrationInfo,
                    lastDRepInteraction,
                    dormantEpochs,
                    latestProposalUpToRegistration,
                    eraFirstEpoch,
                    evaluatedEpoch
            );
        }
    }

    public static int calculateDRepExpiryV10(
            DRepRegistrationInfo registrationInfo,
            @Nullable DRepInteractionInfo lastDRepInteraction,
            Set<Integer> dormantEpochs,
            int evaluatedEpoch
    ) {
        // Choose the epoch to be used as the base for expiry calculation
        int lastActivityEpoch = lastDRepInteraction != null
                ? lastDRepInteraction.epoch()
                : registrationInfo.epoch();

        int dormantCount = (int) dormantEpochs.stream()
                .filter(e -> e > lastActivityEpoch && e <= evaluatedEpoch)
                .count();

        int activityWindow = lastDRepInteraction != null
                ? lastDRepInteraction.dRepActivity()
                : registrationInfo.dRepActivity();

        // Calculate the expiry
        int result = lastActivityEpoch + activityWindow + dormantCount;

        if (result <= evaluatedEpoch) {
            // The DRep is inactive, recalculate expiry
            result = calculateInactiveDRepExpiry(lastActivityEpoch, activityWindow, 0, evaluatedEpoch, dormantEpochs);
        }

        return result;
    }

    public static int calculateDRepExpiryV9(
            DRepRegistrationInfo registrationInfo,
            @Nullable DRepInteractionInfo lastDRepInteraction,
            Set<Integer> dormantEpochs,
            @Nullable ProposalSubmissionInfo latestProposalUpToRegistration,
            int eraFirstEpoch,
            int evaluatedEpoch
    ) {
        // Choose the epoch to be used as the base for expiry calculation
        int lastActivityEpoch = lastDRepInteraction != null
                ? lastDRepInteraction.epoch()
                : registrationInfo.epoch();

        int dormantCount = (int) dormantEpochs.stream()
                .filter(e -> e > lastActivityEpoch && e <= evaluatedEpoch)
                .count();

        int activityWindow = lastDRepInteraction != null
                ? lastDRepInteraction.dRepActivity()
                : registrationInfo.dRepActivity();

        // Calculate the base expiry without v9 bonus
        int baseExpiry = lastActivityEpoch + activityWindow + dormantCount;

        // Only apply v9 bonus if there are no post-registration interactions
        int v9Bonus = 0;
        if (lastDRepInteraction == null) {
            var dormantEpochsToDRepRegistration = dormantEpochs.stream()
                    .filter(e -> e <= registrationInfo.epoch())
                    .collect(Collectors.toSet());

            v9Bonus = computeV9Bonus(registrationInfo, latestProposalUpToRegistration, dormantEpochsToDRepRegistration, eraFirstEpoch);
        }

        int result = baseExpiry + v9Bonus;

        if (result <= evaluatedEpoch) {
            // The DRep is inactive, recalculate expiry
            result = calculateInactiveDRepExpiry(lastActivityEpoch, activityWindow, v9Bonus, evaluatedEpoch, dormantEpochs);
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
     *   <li>DRep registration was in a dormant period</li>
     * </ul>
     *
     * <p>The bonus is determined based on the registration timing:</p>
     * <ul>
     *   <li>If the DRep registered before any proposal was submitted, the bonus equals {@code registeredEpoch - eraFirstEpoch + 1}.</li>
     *   <li>If the DRep registered during a dormant period, the bonus equals the length of that dormant period.</li>
     * </ul>
     *
     * @param registrationInfo               Information about the DRep registration, including epoch, slot, and drepActivity
     * @param latestProposalUpToRegistration Information of the most recent proposal submitted up to and including the DRep registration slot.
     * @param eraFirstEpoch                  First epoch of the Conway era or later.
     * @return Bonus in number of epochs, or 0 if no bonus applies.
     */
    public static int computeV9Bonus(
            DRepRegistrationInfo registrationInfo,
            ProposalSubmissionInfo latestProposalUpToRegistration,
            Set<Integer> dormantEpochsToRegistrationEpoch,
            int eraFirstEpoch) {

        int registeredEpoch = registrationInfo.epoch();
        long registeredSlot = registrationInfo.slot();

        if (latestProposalUpToRegistration == null) {
            return registeredEpoch - eraFirstEpoch + 1;
        }

        int gap = registeredEpoch - latestProposalUpToRegistration.epoch() - latestProposalUpToRegistration.govActionLifeTime();

        if (gap > 0) {
            return gap;
        } else if (registeredEpoch == latestProposalUpToRegistration.epoch() && registeredSlot <= latestProposalUpToRegistration.slot()) {
            var lastDormantPeriod = findLastDormantPeriod(dormantEpochsToRegistrationEpoch, registeredEpoch);
            if (lastDormantPeriod != null) {
                return lastDormantPeriod._2 - lastDormantPeriod._1 + 1;
            }
        }

        return 0;
    }

    /**
     * Recalculates the expiry epoch for a DRep that has become inactive.
     * <p>
     * When a DRep becomes inactive (expiry value at epoch X is less than epoch X), dormant epochs
     * no longer affect the expiry calculation. This method determines the exact epoch
     * just before the DRep became inactive, equivalent to the expiry value before the epoch became inactive
     *
     * @param lastActionEpoch The epoch of the last DRep action (registration, update, or vote)
     * @param activityWindow  The number of active epochs granted after each interaction
     * @param v9Bonus         Additional epochs granted for protocol version 9 (if applicable)
     * @param evaluatedEpoch  The epoch that just ended
     * @param dormantEpochs   Set of epochs considered dormant (no active proposals)
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
     * Finds the last dormant period from a set of dormant epochs.
     * A dormant period is a continuous sequence of dormant epochs.
     *
     * @param dormantEpochs A set of epochs considered dormant (no active proposals).
     * @param maxEpoch      The maximum epoch to consider.
     * @return A tuple containing the start and end epochs of the last dormant period,
     * or null if no dormant period is found.
     */
    private static Tuple<Integer, Integer> findLastDormantPeriod(Set<Integer> dormantEpochs, int maxEpoch) {
        if (dormantEpochs.isEmpty()) {
            return null;
        }

        // Sort dormant epochs in descending order
        List<Integer> sortedEpochs = dormantEpochs.stream()
                .filter(e -> e <= maxEpoch)
                .sorted(Comparator.reverseOrder())
                .toList();

        if (sortedEpochs.isEmpty()) {
            return null;
        }

        int end = sortedEpochs.get(0);
        int start = end;

        // Find the continuous sequence from the end
        for (int i = 1; i < sortedEpochs.size(); i++) {
            int current = sortedEpochs.get(i);
            if (current == start - 1) {
                // This epoch is continuous with the current period
                start = current;
            } else {
                // Break in continuity, we've found the last period
                break;
            }
        }

        return new Tuple<>(start, end);
    }

    /**
     * Contains metadata about a DRep's registration on the Cardano blockchain.
     *
     * @param slot                 The slot at which the registration transaction was included.
     * @param epoch                The epoch corresponding to the registration slot.
     * @param dRepActivity         The {@code dRepActivity} value from protocol parameters at the time of registration.
     * @param protocolMajorVersion The major protocol version at the time of registration.
     */
    public record DRepRegistrationInfo(long slot, int epoch, int dRepActivity, int protocolMajorVersion) {
    }

    /**
     * Represents the interaction performed by a DRep
     * (voting or update).
     *
     * @param epoch        The epoch in which the interaction occurred.
     * @param dRepActivity The {@code dRepActivity} value from protocol parameters at the time of registration.
     */
    public record DRepInteractionInfo(int epoch, int dRepActivity) {
    }

    /**
     * Represents a governance proposal submission
     *
     * @param slot              The slot at which the proposal was submitted.
     * @param epoch             The epoch in which the proposal was submitted.
     * @param govActionLifeTime The {@code govActionLifeTime} value from protocol parameters, the number of epochs the proposal is considered active.
     */
    public record ProposalSubmissionInfo(long slot, int epoch, int govActionLifeTime) {
    }

//    private record DormantPeriod(Optional<Long> endingSlot, int length) {
//    }
}
