package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class CommitteeVotingEvaluatorTest {

    private final CommitteeVotingEvaluator evaluator = new CommitteeVotingEvaluator();

    /**
     * When the committee is in NO_CONFIDENCE state the evaluator must return
     * NOT_PASS_THRESHOLD regardless of member votes.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenCommitteeStateIsNoConfidence() {
        // Committee has a majority YES vote, but is in NO_CONFIDENCE state
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NO_CONFIDENCE)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(
                        member("cold1", "hot1"),
                        member("cold2", "hot2"),
                        member("cold3", "hot3")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1", Vote.YES,
                                "hot2", Vote.YES,
                                "hot3", Vote.YES))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    @Test
    void evaluate_returnsPassThreshold_whenCommitteeStateIsNormal_andMajorityVotesYes() {
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(List.of(
                        member("cold1", "hot1"),
                        member("cold2", "hot2"),
                        member("cold3", "hot3")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1", Vote.YES,
                                "hot2", Vote.YES,
                                "hot3", Vote.NO))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_returnsNotPassThreshold_whenCommitteeStateIsNormal_andMinorityVotesYes() {
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.67")))
                .members(List.of(
                        member("cold1", "hot1"),
                        member("cold2", "hot2"),
                        member("cold3", "hot3")))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1", Vote.YES,
                                "hot2", Vote.NO,
                                "hot3", Vote.NO))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
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

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
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

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
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

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
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

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_memberWithoutHotKey_doesNotAffectAcceptedRatio() {
        // ratio = 1 yes / (1 yes + 1 no) = 0.5, threshold = 0.5 → PASS
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .members(List.of(
                        member("cold1111111111111111111111111111111111111111111111111111", null),
                        member("cold2222222222222222222222222222222222222222222222222222", "hot1111111111111111111111111111111111111111111111111111"),
                        member("cold3333333333333333333333333333333333333333333333333333", "hot2222222222222222222222222222222222222222222222222222")))
                .threshold(new UnitInterval(BigInteger.valueOf(1), BigInteger.valueOf(2)))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1111111111111111111111111111111111111111111111111111", Vote.YES,
                                "hot2222222222222222222222222222222222222222222222222222", Vote.NO))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_registeredMemberWithNoVote_countsAsNo() {
        // ratio = 1 yes / (1 yes + 2 no) ≈ 0.33, threshold = 0.5 → NOT_PASS
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .members(List.of(
                        member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111"),
                        member("cold2222222222222222222222222222222222222222222222222222", "hot2222222222222222222222222222222222222222222222222222"),
                        member("cold3333333333333333333333333333333333333333333333333333", "hot3333333333333333333333333333333333333333333333333333")))
                .threshold(new UnitInterval(BigInteger.valueOf(1), BigInteger.valueOf(2)))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1111111111111111111111111111111111111111111111111111", Vote.YES
                                // hot2, hot3 did not vote → counted as NO
                        ))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    @Test
    void evaluate_abstainVote_isExcludedFromDenominator() {
        // ratio = 1 yes / (1 yes + 1 no) = 0.5, threshold = 0.5 → PASS
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .members(List.of(
                        member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111"),
                        member("cold2222222222222222222222222222222222222222222222222222", "hot2222222222222222222222222222222222222222222222222222"),
                        member("cold3333333333333333333333333333333333333333333333333333", "hot3333333333333333333333333333333333333333333333333333")))
                .threshold(new UnitInterval(BigInteger.valueOf(1), BigInteger.valueOf(2)))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1111111111111111111111111111111111111111111111111111", Vote.YES,
                                "hot2222222222222222222222222222222222222222222222222222", Vote.ABSTAIN,
                                "hot3333333333333333333333333333333333333333333333333333", Vote.NO))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void evaluate_zeroThreshold_alwaysPasses() {
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(ConstitutionCommitteeState.NORMAL)
                .members(List.of(
                        member("cold1111111111111111111111111111111111111111111111111111", "hot1111111111111111111111111111111111111111111111111111")))
                .threshold(new UnitInterval(BigInteger.valueOf(0), BigInteger.valueOf(1)))
                .build();

        VotingData votingData = VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1111111111111111111111111111111111111111111111111111", Vote.NO))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .committee(committee)
                .build();

        VotingStatus result = evaluator.evaluate(votingData, context);

        assertThat(result).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    // -------------------------------------------------------------------------

    private static CommitteeMember member(String coldKey, String hotKey) {
        return CommitteeMember.builder()
                .coldKey(coldKey)
                .hotKey(hotKey)
                .build();
    }
}
