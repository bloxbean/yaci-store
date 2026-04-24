package com.bloxbean.cardano.yaci.store.governancerules.voting.drep;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.TreasuryWithdrawalsAction;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
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

class DRepVotingEvaluatorTest {

    private final DRepVotingEvaluator evaluator = new DRepVotingEvaluator();

    /**
     * When no DReps have participating stake (all inactive or none registered),
     * the accepted ratio is 0, which is below any positive threshold → vote must fail.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenTotalStakeIsZero_andThresholdIsNonZero() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        DrepVoteThresholds thresholds = DrepVoteThresholds.builder()
                .dvtTreasuryWithdrawal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.67")))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.ZERO)
                        .noVoteStake(BigInteger.ZERO)
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(thresholds)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    /**
     * When the required threshold is exactly zero, the vote auto-passes
     * regardless of participating stake.
     */
    @Test
    void evaluate_returnsPassThreshold_whenThresholdIsZero() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        DrepVoteThresholds thresholds = DrepVoteThresholds.builder()
                .dvtTreasuryWithdrawal(UnitIntervalUtil.decimalToUnitInterval(BigDecimal.ZERO))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.ZERO)
                        .noVoteStake(BigInteger.ZERO)
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(thresholds)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    /**
     * Yes stake exceeds threshold: yes=600M, no=400M, threshold=0.51 → ratio=0.60 → passes.
     */
    @Test
    void evaluate_returnsPassThreshold_whenYesStakeMeetsThreshold() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        DrepVoteThresholds thresholds = DrepVoteThresholds.builder()
                .dvtTreasuryWithdrawal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(600_000_000))
                        .noVoteStake(BigInteger.valueOf(400_000_000))
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(thresholds)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    /**
     * Yes stake below threshold: yes=400M, no=600M, threshold=0.67 → ratio=0.40 → fails.
     */
    @Test
    void evaluate_returnsNotPassThreshold_whenYesStakeBelowThreshold() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        DrepVoteThresholds thresholds = DrepVoteThresholds.builder()
                .dvtTreasuryWithdrawal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.67")))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(400_000_000))
                        .noVoteStake(BigInteger.valueOf(600_000_000))
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(thresholds)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
    }

    /**
     * Returns INSUFFICIENT_DATA when drepVotes is null.
     */
    @Test
    void evaluate_returnsInsufficientData_whenDrepVotesIsNull() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        DrepVoteThresholds thresholds = DrepVoteThresholds.builder()
                .dvtTreasuryWithdrawal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.67")))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(null)
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(thresholds)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.INSUFFICIENT_DATA);
    }

    /**
     * Returns INSUFFICIENT_DATA when drepThresholds is null.
     */
    @Test
    void evaluate_returnsInsufficientData_whenDrepThresholdsIsNull() {
        TreasuryWithdrawalsAction govAction = mock(TreasuryWithdrawalsAction.class);
        when(govAction.getType()).thenReturn(GovActionType.TREASURY_WITHDRAWALS_ACTION);

        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(600_000_000))
                        .noVoteStake(BigInteger.valueOf(400_000_000))
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .build();

        VotingEvaluationContext context = VotingEvaluationContext.builder()
                .govAction(govAction)
                .drepThresholds(null)
                .build();

        assertThat(evaluator.evaluate(votingData, context)).isEqualTo(VotingStatus.INSUFFICIENT_DATA);
    }
}
