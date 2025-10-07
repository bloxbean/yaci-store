package com.bloxbean.cardano.yaci.store.governancerules.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProposalUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the set of proposals that must be dropped when crossing an epoch
 * boundary, based on the outcomes of the epoch-end ratification step.
 *
 * <p>Background (ledger semantics):
 * In the Conway ledger, governance actions (proposals) form purpose-specific
 * chains via their {@code prevGovActionId}. At the epoch boundary, the ledger
 * evaluates active proposals and results in actions being either ACCEPTed
 * (ratified and scheduled for enactment), marked as EXPIRED,
 * or left pending to continue into the next epoch. We have the following rules:
 *
 * - If a proposal is ACCEPTed, then all of its competing siblings (same
 *   purpose, sharing the same {@code prevGovActionId}) and the descendants of
 *   those siblings must be dropped.
 * - If a proposal ends up in the EXPIRED set at the epoch boundary (either it
 *   reached its expiry or became invalid after another action was enacted/
 *   expired before it), then any proposal that directly or transitively depends
 *   on it (its descendants along the same purpose chain) can no longer be
 *   enacted and must be dropped.
 * - Informational and Treasury Withdrawal actions do not participate in these
 *   chains (no purpose / no {@code prevGovActionId} dependency in practice),
 *   so they never contribute to drop propagation.
 *
 * <p>Implementation notes:
 * - Purpose grouping is determined by {@link com.bloxbean.cardano.yaci.store.governancerules.util.ProposalUtils#isSamePurpose},
 *   which currently treats Committee actions (NO_CONFIDENCE, UPDATE_COMMITTEE)
 *   as the same purpose and other action types as distinct purposes.
 * - Deduplication is done by {@link GovActionId} to avoid returning the same
 *   proposal multiple times when different paths imply a drop.
 * - Inputs should already exclude proposals that are not active at the epoch
 *   boundary; this service only determines which of those active proposals are
 *   to be dropped in the transition into the next epoch.
 */
public class ProposalDropService {

    /**
     * Determine which proposals should be dropped at the epoch boundary.
     *
     * <p>Rules applied:
     * - For each proposal in {@code expiredProposals}, drop all of its
     *   descendants (same purpose)
     * - For each proposal in {@code ratifiedProposals}, drop its competing
     *   siblings (same purpose, same {@code prevGovActionId}) and those
     *   siblings' descendants
     * - Proposals already in {@code expiredProposals} or
     *   {@code ratifiedProposals} are not included in the returned list.
     *
     * @param proposals            All active proposals considered at the epoch boundary
     * @param expiredProposals     Proposals that ended the epoch in the ledger's
     *                             EXPIRED set (to be removed at the boundary)
     * @param ratifiedProposals    Proposals that ended the epoch as ACCEPT
     * @return List of proposals to be dropped for the next epoch
     */
    public List<Proposal> getProposalsBeDropped(
            List<Proposal> proposals,
            List<Proposal> expiredProposals,
            List<Proposal> ratifiedProposals) {

        Map<GovActionId, Proposal> proposalsBeDropped = new HashMap<>();

        List<Proposal> proposalsExcludingExpiredOrRatified = proposals.stream()
                .filter(p -> !expiredProposals.contains(p) && !ratifiedProposals.contains(p))
                .toList();

        expiredProposals.forEach(proposal ->
                ProposalUtils.findDescendants(proposal, proposalsExcludingExpiredOrRatified)
                        .forEach(p -> proposalsBeDropped.putIfAbsent(p.getGovActionId(), p))
        );

        ratifiedProposals.forEach(proposal ->
                ProposalUtils.findSiblingsAndTheirDescendants(proposal, proposalsExcludingExpiredOrRatified)
                        .forEach(p -> proposalsBeDropped.putIfAbsent(p.getGovActionId(), p))
        );

        return proposalsBeDropped.values().stream().toList();
    }
}
