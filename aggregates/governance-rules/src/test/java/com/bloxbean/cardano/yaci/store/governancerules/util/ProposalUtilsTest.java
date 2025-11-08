package com.bloxbean.cardano.yaci.store.governancerules.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalUtilsTest {

    @Test
    void findSiblings_whenGivenProposalHasNoParent_returnsProposalsWithSamePurpose() {
        GovActionId givenGovActionId = new GovActionId("1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a", 0);
        Proposal givenProposal = proposal(givenGovActionId, null, GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal samePurposeSibling = proposal(new GovActionId("2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b", 0),
                null,
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal differentPurposeSibling = proposal(new GovActionId("3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c", 0),
                null,
                GovActionType.NO_CONFIDENCE);
        Proposal differentParent = proposal(new GovActionId("4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d", 0),
                new GovActionId("5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e", 0),
                GovActionType.PARAMETER_CHANGE_ACTION);

        List<Proposal> proposals = List.of(givenProposal, samePurposeSibling, differentPurposeSibling, differentParent);

        List<Proposal> siblings = ProposalUtils.findSiblings(givenProposal, proposals);

        assertThat(siblings).containsExactly(samePurposeSibling);
    }

    @Test
    void findSiblings_whenGivenProposalHasParent_filtersByParentAndPurpose() {
        GovActionId parentId = new GovActionId("1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a", 0);
        GovActionId otherParentId = new GovActionId("2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b2b", 0);

        Proposal givenProposal = proposal(
                new GovActionId("3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c", 0),
                parentId,
                GovActionType.NO_CONFIDENCE);
        Proposal samePurposeSibling = proposal(
                new GovActionId("4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d4d", 0),
                parentId,
                GovActionType.UPDATE_COMMITTEE);
        Proposal differentPurposeSibling = proposal(
                new GovActionId("5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e", 0),
                parentId,
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal differentParent = proposal(
                new GovActionId("6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f6f", 0),
                otherParentId,
                GovActionType.NO_CONFIDENCE);

        List<Proposal> proposals = List.of(givenProposal, samePurposeSibling, differentPurposeSibling, differentParent);

        List<Proposal> siblings = ProposalUtils.findSiblings(givenProposal, proposals);

        assertThat(siblings).containsExactly(samePurposeSibling);
    }

    @Test
    void findSiblings_whenTypeIsInfoAction_returnsEmptyList() {
        Proposal infoProposal = proposal(
                new GovActionId("7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a", 0),
                null,
                GovActionType.INFO_ACTION);
        Proposal other = proposal(
                new GovActionId("8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b", 0),
                null,
                GovActionType.INFO_ACTION);

        List<Proposal> proposals = List.of(infoProposal, other);

        List<Proposal> siblings = ProposalUtils.findSiblings(infoProposal, proposals);

        assertThat(siblings).isEmpty();
    }

    @Test
    void findDescendants_returnsAllDescendantsMatchingPurpose() {
        GovActionId givenId = new GovActionId("9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c9c", 0);
        Proposal givenProposal = proposal(givenId, null, GovActionType.PARAMETER_CHANGE_ACTION);

        Proposal child1 = proposal(
                new GovActionId("0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d", 0),
                givenId,
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal child2 = proposal(
                new GovActionId("1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e", 0),
                givenId,
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal unrelatedChild = proposal(
                new GovActionId("2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f", 0),
                givenId,
                GovActionType.NO_CONFIDENCE);

        Proposal grandChild1 = proposal(
                new GovActionId("3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a3a", 0),
                child1.getGovActionId(),
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal grandChild2 = proposal(
                new GovActionId("4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b4b", 0),
                child2.getGovActionId(),
                GovActionType.PARAMETER_CHANGE_ACTION);
        Proposal differentPurposeGrandChild = proposal(
                new GovActionId("5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c", 0),
                child2.getGovActionId(),
                GovActionType.NO_CONFIDENCE);

        List<Proposal> proposals = List.of(givenProposal, child1, child2, unrelatedChild, grandChild1, grandChild2, differentPurposeGrandChild);

        List<Proposal> descendants = ProposalUtils.findDescendants(givenProposal, proposals);

        assertThat(descendants).containsExactly(child1, child2, grandChild1, grandChild2);
    }

    @Test
    void findDescendants_whenTypeIsInfoAction_returnsEmptyList() {
        Proposal infoProposal = proposal(
                new GovActionId("7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a7a", 0),
                null,
                GovActionType.INFO_ACTION);

        List<Proposal> proposals = List.of(infoProposal);

        List<Proposal> descendants = ProposalUtils.findDescendants(infoProposal, proposals);

        assertThat(descendants).isEmpty();
    }

    @Test
    void findSiblingsAndTheirDescendants_returnsSiblingsPlusDescendants() {
        GovActionId parentId = new GovActionId("6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d", 0);
        GovActionId otherParentId = new GovActionId("444444446d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d6d", 0);

        Proposal givenProposal = proposal(
                new GovActionId("7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e", 0),
                parentId,
                GovActionType.NO_CONFIDENCE);

        Proposal sibling1 = proposal(
                new GovActionId("8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a8a", 0),
                parentId,
                GovActionType.NO_CONFIDENCE);

        Proposal sibling2 = proposal(
                new GovActionId("9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b9b", 0),
                parentId,
                GovActionType.UPDATE_COMMITTEE);
        Proposal unrelatedProposal = proposal(
                new GovActionId("0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c", 0),
                otherParentId,
                GovActionType.NO_CONFIDENCE);

        Proposal sibling1Child = proposal(
                new GovActionId("1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d1d", 0),
                sibling1.getGovActionId(),
                GovActionType.UPDATE_COMMITTEE);
        Proposal sibling2Child = proposal(
                new GovActionId("2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e", 0),
                sibling2.getGovActionId(),
                GovActionType.NO_CONFIDENCE);
        Proposal unrelatedChild = proposal(
                new GovActionId("3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f3f", 0),
                otherParentId,
                GovActionType.NO_CONFIDENCE);

        List<Proposal> proposals = List.of(
                givenProposal,
                sibling1,
                sibling2,
                unrelatedProposal,
                sibling1Child,
                sibling2Child,
                unrelatedChild
        );

        List<Proposal> result = ProposalUtils.findSiblingsAndTheirDescendants(givenProposal, proposals);

        assertThat(result).containsExactly(sibling1, sibling1Child, sibling2, sibling2Child);
    }

    @Test
    void isSamePurpose_respectsPurposeGroups() {
        assertThat(ProposalUtils.isSamePurpose(GovActionType.PARAMETER_CHANGE_ACTION, GovActionType.PARAMETER_CHANGE_ACTION)).isTrue();
        assertThat(ProposalUtils.isSamePurpose(GovActionType.NO_CONFIDENCE, GovActionType.UPDATE_COMMITTEE)).isTrue();
        assertThat(ProposalUtils.isSamePurpose(GovActionType.UPDATE_COMMITTEE, GovActionType.NO_CONFIDENCE)).isTrue();
        assertThat(ProposalUtils.isSamePurpose(GovActionType.PARAMETER_CHANGE_ACTION, GovActionType.NO_CONFIDENCE)).isFalse();
        assertThat(ProposalUtils.isSamePurpose(GovActionType.INFO_ACTION, GovActionType.TREASURY_WITHDRAWALS_ACTION)).isFalse();
    }

    private static Proposal proposal(GovActionId id, GovActionId prevId, GovActionType type) {
        return Proposal.builder()
                .govActionId(id)
                .prevGovActionId(prevId)
                .type(type)
                .build();
    }
}
