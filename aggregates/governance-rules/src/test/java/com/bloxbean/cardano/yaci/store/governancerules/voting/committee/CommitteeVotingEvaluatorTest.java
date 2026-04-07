package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
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

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
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

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
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

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    private static CommitteeMember member(String coldKey, String hotKey) {
        return CommitteeMember.builder()
                .coldKey(coldKey)
                .hotKey(hotKey)
                .build();
    }
}
