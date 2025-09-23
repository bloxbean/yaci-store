package com.bloxbean.cardano.yaci.store.governancerules.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;

import java.util.*;

/**
 * Utilities for finding proposal relationships and dependencies.
 */
public class ProposalUtils {

    /**
     * Finds all descendants and siblings of a specific proposal.
     *
     * @param proposal     The proposal for which descendants and/or siblings are to be found.
     * @param proposalList The list of proposals.
     * @return A list of proposals that are descendants and siblings of the given proposal.
     */
    public static List<Proposal> findDescendantsAndSiblings(Proposal proposal, List<Proposal> proposalList) {
        if (proposal.getType() == GovActionType.INFO_ACTION || proposal.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        List<Proposal> result = new ArrayList<>();
        GovActionType type = proposal.getType();

        // Add descendants
        result.addAll(findDescendants(proposal, proposalList, type));

        GovActionId parentId = proposal.getPrevGovActionId();
        if (parentId != null) {
            // Find siblings with the same parent and type
            result.addAll(
                    proposalList.stream()
                            .filter(p -> parentId.equals(p.getPrevGovActionId()) && !p.equals(proposal)
                                    && isSamePurpose(p.getType(), type))
                            .toList()
            );
        } else {
            // If no parent, find siblings among root nodes
            result.addAll(
                    proposalList.stream()
                            .filter(p -> p.getPrevGovActionId() == null && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                            .toList()
            );
        }

        return result;
    }

    /**
     * Find siblings of a proposal
     * @param proposal The proposal for which siblings are to be found.
     * @param proposalList The list of proposals.
     * @return A list of proposals that are descendants and siblings of the given proposal.
     */
    public static List<Proposal> findSiblings(Proposal proposal, List<Proposal> proposalList) {
        GovActionType type = proposal.getType();

        if (type == GovActionType.INFO_ACTION || type == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        GovActionId parentId = proposal.getPrevGovActionId();

        if (parentId == null) {
            return proposalList.stream()
                    .filter(p -> p.getPrevGovActionId() == null && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                    .toList();
        } else {
            return proposalList.stream()
                    .filter(p -> parentId.equals(p.getPrevGovActionId()) && !p.equals(proposal) && isSamePurpose(p.getType(), type))
                    .toList();
        }
    }

    /**
     * Find descendants of a proposal
     * @param proposal The proposal for which siblings are to be found.
     * @param proposalList The list of proposals.
     * @return A list of proposals that are descendants of the given proposal.
     */
    public static List<Proposal> findDescendants(Proposal proposal, List<Proposal> proposalList) {
        GovActionType type = proposal.getType();

        return new ArrayList<>(findDescendants(proposal, proposalList, type));
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
                    .filter(p -> current.getGovActionId().equals(p.getPrevGovActionId()) && isSamePurpose(p.getType(), type))
                    .toList();

            descendants.addAll(children);
            queue.addAll(children);
        }

        return descendants;
    }

    /**
     * Finds siblings and their descendants of a proposal.
     *
     * @param proposal         The proposal for which siblings and their descendants are to be found.
     * @param proposalList     The list of proposals.
     * @return A list of proposals that are siblings and descendants of siblings of the proposal.
     */
    public static List<Proposal> findSiblingsAndTheirDescendants(Proposal proposal, List<Proposal> proposalList) {
        if (proposal.getType() == GovActionType.INFO_ACTION || proposal.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION) {
            return Collections.emptyList();
        }

        List<Proposal> result = new ArrayList<>();

        // First, find all siblings of the proposal
        List<Proposal> siblings = findSiblings(proposal, proposalList);

        // For each sibling, find all its descendants and add both sibling and descendants to result
        for (Proposal sibling : siblings) {
            result.add(sibling);
            result.addAll(findDescendants(sibling, proposalList));
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
