package com.bloxbean.cardano.yaci.store.governanceaggr.util;

import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DRepExpiryUtil {

    /**
     * Calculates the expiry epoch for a DRep.
     *
     * <p>The expiry epoch is computed using:</p>
     * <ul>
     *   <li>The latest interaction or registration epoch.</li>
     *   <li>The activity window (dRepActivity) from registration or interaction.</li>
     *   <li>The number of dormant epochs since the last interaction.</li>
     *   <li>And a bonus period if protocol version is 9 and the DRep hasn't interacted since registration.</li>
     * </ul>
     *
     * @param registrationInfo          The DRep registration details.
     * @param lastDRepInteraction       The last interaction info, or {@code null} if none.
     * @param dormantEpochs             Set of dormant epochs (no active proposals).
     * @param proposalsUpToRegistration All proposals up to DRep registration epoch.
     * @param eraFirstEpoch             The first epoch of the Conway era.
     * @param currentEpoch              The current epoch.
     * @return The computed expiry epoch.
     */
    public static int calculateDRepExpiry(
            DRepRegistrationInfo registrationInfo,
            @Nullable DRepInteractionInfo lastDRepInteraction,
            Set<Integer> dormantEpochs,
            List<ProposalSubmissionInfo> proposalsUpToRegistration,
            int eraFirstEpoch,
            int currentEpoch
    ) {
        int lastInteractionEpoch = lastDRepInteraction != null
                ? lastDRepInteraction.epoch()
                : registrationInfo.epoch();

        int dormantCount = (int) dormantEpochs.stream()
                .filter(e -> e > lastInteractionEpoch && e <= currentEpoch)
                .count();

        int activityWindow = lastDRepInteraction != null
                ? lastDRepInteraction.dRepActivity()
                : registrationInfo.dRepActivity();
        int baseExpiry = lastInteractionEpoch + activityWindow + dormantCount;

        if (registrationInfo.protocolMajorVersion() >= 10) {
            return baseExpiry;
        }

        if (lastDRepInteraction != null) {
            return baseExpiry;
        }

        int bonus = computeV9Bonus(registrationInfo, proposalsUpToRegistration, eraFirstEpoch);
        return baseExpiry + bonus;
    }

    /**
     * Computes the bonus epochs for DReps in protocol version 9
     *
     * <p>The bonus is granted only if:</p>
     * <ul>
     *   <li>The DRep has not interacted after registration.</li>
     *   <li>And a dormant period can be identified prior to or during registration.</li>
     * </ul>
     *
     * <p>The bonus can either be:</p>
     * <ul>
     *   <li>Full bonus: {@code registeredEpoch - eraFirstEpoch + 1} if registered before any proposal.</li>
     *   <li>Partial bonus: Length of last dormant period if registered within or before it ended.</li>
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

        if (proposalsUpToRegistration.isEmpty()) {
            return registeredEpoch - eraFirstEpoch + 1;
        }

        ProposalSubmissionInfo firstProposal = proposalsUpToRegistration.stream()
                .min(Comparator.comparingInt(ProposalSubmissionInfo::epoch)
                        .thenComparingLong(ProposalSubmissionInfo::slot))
                .get();

        if (registeredEpoch < firstProposal.epoch()
                || (registeredEpoch == firstProposal.epoch()
                && registeredSlot <= firstProposal.slot())) {
            return registeredEpoch - eraFirstEpoch + 1;
        }

        List<ProposalSubmissionInfo> sortedProposals = proposalsUpToRegistration.stream()
                .sorted(Comparator
                        .comparingInt(ProposalSubmissionInfo::epoch).reversed()
                        .thenComparingLong(ProposalSubmissionInfo::slot).reversed())
                .toList();

        DormantPeriod dormantPeriod = findLastDormantPeriod(sortedProposals, registeredEpoch);

        if (dormantPeriod == null) return 0;

        if (dormantPeriod.endingSlot().isEmpty()) {
            return dormantPeriod.length();
        } else {
            return registeredSlot <= dormantPeriod.endingSlot().get() ? dormantPeriod.length() : 0;
        }
    }

    /**
     * Finds the last dormant period before or at the registration epoch.
     *
     * @param sortedProposalsDesc Proposals sorted descending by epoch and slot.
     * @param registeredEpoch     The epoch of registration.
     * @return The last dormant period or {@code null} if none.
     */
    private static DormantPeriod findLastDormantPeriod(
            List<ProposalSubmissionInfo> sortedProposalsDesc,
            int registeredEpoch) {

        int previousEpoch = registeredEpoch;
        Long previousSlot = null;

        for (ProposalSubmissionInfo proposal : sortedProposalsDesc) {
            int gap = previousEpoch - proposal.epoch() - proposal.govActionLifeTime();
            if (gap > 0) {
                if (previousSlot == null) {
                    return new DormantPeriod(Optional.empty(), gap);
                } else {
                    return new DormantPeriod(Optional.of(previousSlot), gap);
                }
            }
            previousEpoch = proposal.epoch();
            previousSlot = proposal.slot();
        }

        return null;
    }

    public record DRepRegistrationInfo(long slot, int epoch, int dRepActivity, int protocolMajorVersion) {
    }

    public record DRepInteractionInfo(int epoch, int dRepActivity) {
    }

    public record ProposalSubmissionInfo(long slot, int epoch, int govActionLifeTime) {
    }

    private record DormantPeriod(Optional<Long> endingSlot, int length) {
    }

}
