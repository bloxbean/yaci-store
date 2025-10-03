package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoteTallyCalculatorTest {

    @Test
    void computeDRepTalliesWhenVotesMissingReturnsZero() {
        VoteTallies.DRepTallies tallies = VoteTallyCalculator.computeDRepTallies(null, GovActionType.NO_CONFIDENCE);

        assertEquals(BigInteger.ZERO, tallies.getTotalYesStake());
        assertEquals(BigInteger.ZERO, tallies.getTotalNoStake());
    }

    @Test
    // Ensures a NO_CONFIDENCE action folds no-confidence stake into the total yes stake
    void computeDRepTalliesWhenActionIsNoConfidence() {
        VotingData.DRepVotes votes = VotingData.DRepVotes.builder()
                .yesVoteStake(BigInteger.valueOf(100))
                .noConfidenceStake(BigInteger.valueOf(30))
                .noVoteStake(BigInteger.valueOf(20))
                .doNotVoteStake(BigInteger.valueOf(10))
                .build();

        VoteTallies.DRepTallies tallies = VoteTallyCalculator.computeDRepTallies(votes, GovActionType.NO_CONFIDENCE);

        assertEquals(BigInteger.valueOf(130), tallies.getTotalYesStake());
        assertEquals(BigInteger.valueOf(60), tallies.getTotalNoStake());
    }

    @Test
    // Ensures a NO_CONFIDENCE action folds no-confidence stake into the total yes stake
    void computeSPOTalliesWhenActionIsNoConfidence() {
        VotingData.SPOVotes votes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(100))
                .delegateToAutoAbstainDRepStake(BigInteger.valueOf(15))
                .delegateToNoConfidenceDRepStake(BigInteger.valueOf(40))
                .abstainVoteStake(BigInteger.valueOf(10))
                .doNotVoteStake(BigInteger.valueOf(20))
                .totalStake(BigInteger.valueOf(200))
                .build();

        VoteTallies.SPOTallies tallies = VoteTallyCalculator.computeSPOTallies(votes, GovActionType.NO_CONFIDENCE, true);

        assertEquals(BigInteger.valueOf(140), tallies.getTotalYesStake());
        assertEquals(BigInteger.valueOf(45), tallies.getTotalAbstainStake());
        assertEquals(BigInteger.valueOf(15), tallies.getTotalNoStake());
    }

    @Test
    /* Verify: After the bootstrap period if an SPO didn't vote, it will be considered as a `No` vote by default.
    -- The only exceptions are when a pool delegated to an `AlwaysNoConfidence` or an `AlwaysAbstain` DRep . */
    void computeSPOTalliesPostBootstrapPhaseWhenActionIsNotHardForkInit() {
        VotingData.SPOVotes votes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(120))
                .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                .abstainVoteStake(BigInteger.valueOf(50))
                .doNotVoteStake(BigInteger.valueOf(10))
                .totalStake(BigInteger.valueOf(200))
                .build();

        VoteTallies.SPOTallies tallies = VoteTallyCalculator.computeSPOTallies(votes, GovActionType.UPDATE_COMMITTEE, false);

        assertEquals(BigInteger.valueOf(120), tallies.getTotalYesStake());
        assertEquals(BigInteger.valueOf(50), tallies.getTotalAbstainStake());
        assertEquals(BigInteger.valueOf(30), tallies.getTotalNoStake());
    }

    @Test
    void computeCommitteeTalliesCountsEachMemberSharingHotKey() {
        List<CommitteeMember> members = List.of(
                member("111111f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "aaaaaad4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64"),
                member("222222f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "aaaaaad4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64"),
                member("333333f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "bbbbbbd4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64"),
                member("444444f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "cccccc4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64"),
                member("555555f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "dddddd4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64"),
                member("666666f83e9af24813e6cb368df6a80d38951b2a334dfcdf26815558", "eeeeee4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64")
        );

        Map<String, Vote> votes = Map.of(
                "aaaaaad4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64", Vote.YES,
                "bbbbbbd4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64", Vote.ABSTAIN,
                "cccccc4b9a2e70e88965d91dd69be182d5605b23bb5250b1c94bf64", Vote.NO
        );

        VoteTallies.CommitteeTallies tallies = VoteTallyCalculator.computeCommitteeTallies(votes, members);

        assertEquals(2, tallies.getYesCount());
        assertEquals(1, tallies.getNoCount());
        assertEquals(1, tallies.getAbstainCount());
        assertEquals(2, tallies.getDoNotVoteCount());
    }

    private static CommitteeMember member(String coldKey, String hotKey) {
        return CommitteeMember.builder()
                .coldKey(coldKey)
                .hotKey(hotKey)
                .build();
    }
}
