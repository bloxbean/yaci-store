package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommitteeVotingEvaluatorTest {

    private static final CommitteeVotingEvaluator EVALUATOR = new CommitteeVotingEvaluator();

    @Test
    void evaluate_memberWithoutHotKey_doesNotAffectAcceptedRatio() {
        List<CommitteeMember> members = List.of(
                member("cold1111111111111111111111111111111111111111111111111111", null),
                member("cold2222222222222222222222222222222222222222222222222222", "hot1111111111111111111111111111111111111111111111111111"),
                member("cold3333333333333333333333333333333333333333333333333333", "hot2222222222222222222222222222222222222222222222222222")
        );

        Map<String, Vote> votes = Map.of(
                "hot1111111111111111111111111111111111111111111111111111", Vote.YES,
                "hot2222222222222222222222222222222222222222222222222222", Vote.NO
        );

        // ratio = 1 yes / (1 yes + 1 no) = 0.5, threshold = 0.5 → PASS
        VotingStatus result = evaluate(members, votes, 1, 2);
        assertEquals(VotingStatus.PASS_THRESHOLD, result);
    }

    @Test
    void evaluate_registeredMemberWithNoVote_countsAsNo() {
        List<CommitteeMember> members = List.of(
                member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111"),
                member("cold2222222222222222222222222222222222222222222222222222", "hot2222222222222222222222222222222222222222222222222222"),
                member("cold3333333333333333333333333333333333333333333333333333", "hot3333333333333333333333333333333333333333333333333333")
        );

        Map<String, Vote> votes = Map.of(
                "hot1111111111111111111111111111111111111111111111111111", Vote.YES
                // hot2, hot3 did not vote → counted as NO
        );

        // ratio = 1 yes / (1 yes + 2 no) ≈ 0.33, threshold = 0.5 → NOT_PASS
        VotingStatus result = evaluate(members, votes, 1, 2);
        assertEquals(VotingStatus.NOT_PASS_THRESHOLD, result);
    }

    @Test
    void evaluate_abstainVote_isExcludedFromDenominator() {
        List<CommitteeMember> members = List.of(
                member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111"),
                member("cold2222222222222222222222222222222222222222222222222222", "hot2222222222222222222222222222222222222222222222222222"),
                member("cold3333333333333333333333333333333333333333333333333333", "hot3333333333333333333333333333333333333333333333333333")
        );

        Map<String, Vote> votes = Map.of(
                "hot1111111111111111111111111111111111111111111111111111", Vote.YES,
                "hot2222222222222222222222222222222222222222222222222222", Vote.ABSTAIN,
                "hot3333333333333333333333333333333333333333333333333333", Vote.NO
        );

        // ratio = 1 yes / (1 yes + 1 no) = 0.5, threshold = 0.5 → PASS
        VotingStatus result = evaluate(members, votes, 1, 2);
        assertEquals(VotingStatus.PASS_THRESHOLD, result);
    }

    @Test
    void evaluate_zeroThreshold_alwaysPasses() {
        List<CommitteeMember> members = List.of(
                member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111")
        );

        Map<String, Vote> votes = Map.of(
                "hot1111111111111111111111111111111111111111111111111111", Vote.NO
        );

        VotingStatus result = evaluate(members, votes, 0, 1);
        assertEquals(VotingStatus.PASS_THRESHOLD, result);
    }

    // -------------------------------------------------------------------------

    private VotingStatus evaluate(List<CommitteeMember> members, Map<String, Vote> votes,
                                   long thresholdNumerator, long thresholdDenominator) {
        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder().votes(votes).build())
                .build();

        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .members(members)
                .threshold(new UnitInterval(BigInteger.valueOf(thresholdNumerator),
                        BigInteger.valueOf(thresholdDenominator)))
                .build();

        VotingEvaluationContext ctx = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        return EVALUATOR.evaluate(votingData, ctx);
    }

    private static CommitteeMember member(String coldKey, String hotKey) {
        return CommitteeMember.builder()
                .coldKey(coldKey)
                .hotKey(hotKey)
                .build();
    }
}