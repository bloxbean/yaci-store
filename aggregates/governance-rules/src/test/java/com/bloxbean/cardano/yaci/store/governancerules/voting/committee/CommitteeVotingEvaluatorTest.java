package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    // --- committeeMinSize tests
    @Test
    void evaluate_returnsNotPassThreshold_whenCommitteeBelowMinSize_postBootstrap() {
        // 2 active members, committeeMinSize = 5, post-bootstrap -> NOT_PASS_THRESHOLD
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(member("cold1", "hot1"), member("cold2", "hot2")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of("hot1", Vote.YES, "hot2", Vote.YES))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .isInBootstrapPhase(false)
                .committeeMinSize(5)
                .build();

        assertThat(EVALUATOR.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    @Test
    void evaluate_returnsPassThreshold_whenCommitteeMeetsMinSize_postBootstrap() {
        // 5 active members, committeeMinSize = 5, sufficient YES votes -> PASS_THRESHOLD
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(
                        member("cold1", "hot1"), member("cold2", "hot2"), member("cold3", "hot3"),
                        member("cold4", "hot4"), member("cold5", "hot5")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1", Vote.YES, "hot2", Vote.YES, "hot3", Vote.YES,
                                "hot4", Vote.YES, "hot5", Vote.ABSTAIN))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .isInBootstrapPhase(false)
                .committeeMinSize(5)
                .build();

        assertThat(EVALUATOR.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_skipsMinSizeCheck_duringBootstrapPhase() {
        // 2 active members, committeeMinSize = 5, bootstrap phase -> check skipped, PASS_THRESHOLD
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(member("cold1", "hot1"), member("cold2", "hot2")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of("hot1", Vote.YES, "hot2", Vote.YES))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .isInBootstrapPhase(true)
                .committeeMinSize(5)
                .build();

        assertThat(EVALUATOR.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_skipsMinSizeCheck_whenCommitteeMinSizeIsNull() {
        // 2 active members, committeeMinSize = null, post-bootstrap -> check skipped, PASS_THRESHOLD
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(member("cold1", "hot1"), member("cold2", "hot2")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of("hot1", Vote.YES, "hot2", Vote.YES))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .isInBootstrapPhase(false)
                .committeeMinSize(null)
                .build();

        assertThat(EVALUATOR.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    private static CommitteeMember member(String coldKey, String hotKey) {
        return CommitteeMember.builder()
                .coldKey(coldKey)
                .hotKey(hotKey)
                .build();
    }
}