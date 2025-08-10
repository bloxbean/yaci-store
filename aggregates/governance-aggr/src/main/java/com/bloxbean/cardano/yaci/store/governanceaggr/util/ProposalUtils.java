package com.bloxbean.cardano.yaci.store.governanceaggr.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;

import java.util.*;

public class ProposalUtils {

    /**
     * Finds all descendants and siblings of a specific proposal.
     *
     * @param proposal     The proposal for which descendants and/or siblings are to be found.
     * @param allProposals The list of all proposals.
     * @return A list of proposals that are descendants and siblings of the given proposal.
     */
    public static List<Proposal> findDescendantsAndSiblings(Proposal proposal, List<Proposal> allProposals) {
        // Handle types that don't belong to any purpose tree
        if (proposal.getType() == GovActionType.INFO_ACTION || proposal.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        List<Proposal> result = new ArrayList<>();
        GovActionType type = proposal.getType();

        // Add descendants
        result.addAll(findDescendants(proposal, allProposals, type));

        GovActionId parentId = proposal.getPreviousGovActionId();
        if (parentId != null) {
            // Find siblings with the same parent and type
            result.addAll(
                    allProposals.stream()
                            .filter(p -> parentId.equals(p.getPreviousGovActionId()) && !p.equals(proposal)
                                    && isSamePurpose(p.getType(), type))
                            .toList()
            );
        } else {
            // If no parent, find siblings among root nodes
            result.addAll(
                    allProposals.stream()
                            .filter(p -> p.getPreviousGovActionId() == null && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                            .toList()
            );
        }

        return result;
    }

    /**
     * Find siblings of a proposal
     * @param proposal
     * @param allProposals
     * @return
     */
    public static List<Proposal> findSiblings(Proposal proposal, List<Proposal> allProposals) {
        GovActionType type = proposal.getType();

        if (type == GovActionType.INFO_ACTION || type == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        GovActionId parentId = proposal.getPreviousGovActionId();

        if (parentId == null) {
            return allProposals.stream()
                    .filter(p -> p.getPreviousGovActionId() == null && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                    .toList();
        } else {
            return allProposals.stream()
                    .filter(p -> parentId.equals(p.getPreviousGovActionId()) && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                    .toList();
        }
    }

    /**
     * Find descendants of a proposal
     * @param proposal
     * @param allProposals
     * @return
     */
    public static List<Proposal> findDescendants(Proposal proposal, List<Proposal> allProposals) {
        GovActionType type = proposal.getType();

        return new ArrayList<>(findDescendants(proposal, allProposals, type));
    }

    private static List<Proposal> findDescendants(Proposal rootProposal, List<Proposal> allProposals, GovActionType type) {
        if (type == GovActionType.INFO_ACTION || type == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        List<Proposal> descendants = new ArrayList<>();
        Queue<Proposal> queue = new LinkedList<>();
        queue.add(rootProposal);

        while (!queue.isEmpty()) {
            Proposal current = queue.poll();
            List<Proposal> children = allProposals.stream()
                    .filter(p -> current.getGovActionId().equals(p.getPreviousGovActionId()) && isSamePurpose(p.getType(), type))
                    .toList();

            descendants.addAll(children);
            queue.addAll(children);
        }

        return descendants;
    }

    /**
     * Finds siblings and their descendants of a ratified proposal.
     *
     * @param ratifiedProposal The ratified proposal for which siblings and their descendants are to be found.
     * @param allProposals     The list of all proposals.
     * @return A list of proposals that are siblings and descendants of siblings of the ratified proposal.
     */
    public static List<Proposal> findSiblingsAndTheirDescendants(Proposal ratifiedProposal, List<Proposal> allProposals) {
        if (ratifiedProposal.getType() == GovActionType.INFO_ACTION || ratifiedProposal.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        List<Proposal> result = new ArrayList<>();

        // First, find all siblings of the ratified proposal
        List<Proposal> siblings = findSiblings(ratifiedProposal, allProposals);

        // For each sibling, find all its descendants and add both sibling and descendants to result
        for (Proposal sibling : siblings) {
            result.add(sibling);
            result.addAll(findDescendants(sibling, allProposals));
        }

        return result;
    }

    /**
     * Determines if two governance action types belong to the same purpose group.
     * Purpose groups:
     * - PParamUpdatePurpose: PARAMETER_CHANGE_ACTION
     * - HardForkPurpose: HARD_FORK_INITIATION_ACTION
     * - CommitteePurpose: NO_CONFIDENCE, UPDATE_COMMITTEE
     * - ConstitutionPurpose: NEW_CONSTITUTION_ACTION
     * - No purpose: TREASURY_WITHDRAWALS_ACTION, INFO_ACTION
     *
     * @param type1 First governance action type
     * @param type2 Second governance action type
     * @return true if both types belong to the same purpose group
     */
    public static boolean isSamePurpose(GovActionType type1, GovActionType type2) {
        if (type1 == type2) {
            return true;
        }

        // CommitteePurpose: NO_CONFIDENCE and UPDATE_COMMITTEE belong to the same purpose
        if ((type1 == GovActionType.NO_CONFIDENCE && type2 == GovActionType.UPDATE_COMMITTEE) ||
            (type1 == GovActionType.UPDATE_COMMITTEE && type2 == GovActionType.NO_CONFIDENCE)) {
            return true;
        }

        return false;
    }

}
