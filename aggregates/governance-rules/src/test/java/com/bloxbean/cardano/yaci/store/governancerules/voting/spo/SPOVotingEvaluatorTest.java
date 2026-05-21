package com.bloxbean.cardano.yaci.store.governancerules.voting.spo;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SPOVotingEvaluatorTest {

    private final SPOVotingEvaluator evaluator = new SPOVotingEvaluator();

    /**
     * When all active SPO stake is counted as abstain, the denominator (total - abstain)
     * becomes zero and the acceptance ratio is effectively 0, which is below any positive
     * threshold → returns NOT_PASS_THRESHOLD.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenAllStakeAbstains_andThresholdIsNonZero() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.ZERO)
                        .abstainVoteStake(BigInteger.valueOf(100_000_000))
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .totalStake(BigInteger.valueOf(100_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    /**
     * During the bootstrap phase, pools that did not vote are counted as abstain.
     * If all pools do not vote, the acceptance ratio is effectively 0 → returns NOT_PASS_THRESHOLD.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenAllPoolsDoNotVote_duringBootstrap_andThresholdIsNonZero() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.ZERO)
                        .abstainVoteStake(BigInteger.ZERO)
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.valueOf(100_000_000))
                        .totalStake(BigInteger.valueOf(100_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(true)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    /**
     * When the required threshold is exactly zero, the vote auto-passes
     * regardless of participating stake.
     */
    @Test
    void evaluate_returnsPassThreshold_whenThresholdIsZero() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(BigDecimal.ZERO))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.ZERO)
                        .abstainVoteStake(BigInteger.valueOf(100_000_000))
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .totalStake(BigInteger.valueOf(100_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    /**
     * Yes stake exceeds threshold: yes=600M, abstain=100M, total=1000M
     * → ratio = 600/(1000-100) ≈ 0.67 ≥ 0.51 → passes.
     */
    @Test
    void evaluate_returnsPassThreshold_whenYesStakeMeetsThreshold() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(600_000_000))
                        .abstainVoteStake(BigInteger.valueOf(100_000_000))
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.valueOf(300_000_000))
                        .totalStake(BigInteger.valueOf(1_000_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    /**
     * Yes stake below threshold: yes=400M, abstain=0, total=1000M
     * → ratio = 400/1000 = 0.40 < 0.67 → not pass.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenYesStakeBelowThreshold() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.67")))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(400_000_000))
                        .abstainVoteStake(BigInteger.ZERO)
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.valueOf(600_000_000))
                        .totalStake(BigInteger.valueOf(1_000_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    /**
     * Returns INSUFFICIENT_DATA when spoVotes is null.
     */
    @Test
    void evaluate_returnsInsufficientData_whenSpoVotesIsNull() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        PoolVotingThresholds thresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        VotingData votingData = VotingData.builder()
                .spoVotes(null)
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(thresholds)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.INSUFFICIENT_DATA);
    }

    /**
     * Returns INSUFFICIENT_DATA when poolThresholds is null.
     */
    @Test
    void evaluate_returnsInsufficientData_whenPoolThresholdsIsNull() {
        NoConfidence govAction = mock(NoConfidence.class);
        when(govAction.getType()).thenReturn(GovActionType.NO_CONFIDENCE);

        VotingData votingData = VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(600_000_000))
                        .abstainVoteStake(BigInteger.ZERO)
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.valueOf(400_000_000))
                        .totalStake(BigInteger.valueOf(1_000_000_000))
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .poolThresholds(null)
                .isInBootstrapPhase(false)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.INSUFFICIENT_DATA);
    }
}
