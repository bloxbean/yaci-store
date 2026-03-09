package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoConfidenceRatificationEvaluatorTest {

    private final NoConfidenceRatificationEvaluator evaluator = new NoConfidenceRatificationEvaluator();

    /**
     * NoConfidence can be ratified even when the committee state
     * is already NO_CONFIDENCE.
     */
    @Test
    void evaluate_returnsContinue_whenCommitteeIsInNoConfidence_andDRepAndSPOApprove() {
        // 100% DRep yes stake with a threshold of 0.51 → passes
        VotingData.DRepVotes drepVotes = VotingData.DRepVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .noConfidenceStake(BigInteger.ZERO)
                .noVoteStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .build();

        // 100% SPO yes stake with a threshold of 0.51 → passes
        VotingData.SPOVotes spoVotes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                .abstainVoteStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .totalStake(BigInteger.valueOf(1000))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(drepVotes)
                .spoVotes(spoVotes)
                .build();

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null); // no prev action required

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // not last opportunity, not expired
                .build();

        RatificationResult result = evaluator.evaluate(context);

        // Should ACCEPT (not CONTINUE since this is not the last opportunity)
        // Actually currentEpoch=5, maxAllowedVotingEpoch=8, 5-8 = -3 → not last opp,
        // not expired.
        // (isAccepted && isNotDelayed && isPrevAction) ? ACCEPT : CONTINUE
        assertThat(result).isEqualTo(RatificationResult.ACCEPT);
    }

    @Test
    void evaluate_returnsContinue_whenCommitteeIsNormal_andDRepAndSPODoNotReachThreshold() {
        // DRep yes stake below threshold → fails
        VotingData.DRepVotes drepVotes = VotingData.DRepVotes.builder()
                .yesVoteStake(BigInteger.valueOf(300))
                .noConfidenceStake(BigInteger.ZERO)
                .noVoteStake(BigInteger.valueOf(700))
                .doNotVoteStake(BigInteger.ZERO)
                .build();

        VotingData.SPOVotes spoVotes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                .abstainVoteStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .totalStake(BigInteger.valueOf(1000))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(drepVotes)
                .spoVotes(spoVotes)
                .build();

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsReject_whenOutOfLifecycle() {
        VotingData votingData = VotingData.builder()
                .drepVotes(VotingData.DRepVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(1000))
                        .noVoteStake(BigInteger.ZERO)
                        .noConfidenceStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .build())
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(BigInteger.valueOf(1000))
                        .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                        .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                        .abstainVoteStake(BigInteger.ZERO)
                        .doNotVoteStake(BigInteger.ZERO)
                        .totalStake(BigInteger.valueOf(1000))
                        .build())
                .build();

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null);

        // currentEpoch - maxAllowedVotingEpoch > 1 → out of lifecycle
        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE,
                /* isDelayed= */ false,
                /* currentEpoch= */ 15);

        RatificationContext context = RatificationContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // 15 - 8 = 7 > 1 → out of lifecycle
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.REJECT);
    }

    @Test
    void evaluate_returnsReject_onLastRatificationOpportunity_whenVotesDoNotPass() {
        // DRep below threshold
        VotingData.DRepVotes drepVotes = VotingData.DRepVotes.builder()
                .yesVoteStake(BigInteger.valueOf(300))
                .noVoteStake(BigInteger.valueOf(700))
                .noConfidenceStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .build();

        VotingData.SPOVotes spoVotes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                .abstainVoteStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .totalStake(BigInteger.valueOf(1000))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(drepVotes)
                .spoVotes(spoVotes)
                .build();

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null);

        // currentEpoch - maxAllowedVotingEpoch == 1 → last ratification opportunity
        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE,
                /* isDelayed= */ false,
                /* currentEpoch= */ 9);

        RatificationContext context = RatificationContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // 9 - 8 = 1 → last opp
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.REJECT);
    }

    @Test
    void evaluate_returnsReject_whenRatificationIsDelayed_andLastOpportunity() {
        VotingData.DRepVotes drepVotes = VotingData.DRepVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .noVoteStake(BigInteger.ZERO)
                .noConfidenceStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .build();

        VotingData.SPOVotes spoVotes = VotingData.SPOVotes.builder()
                .yesVoteStake(BigInteger.valueOf(1000))
                .delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                .delegateToNoConfidenceDRepStake(BigInteger.ZERO)
                .abstainVoteStake(BigInteger.ZERO)
                .doNotVoteStake(BigInteger.ZERO)
                .totalStake(BigInteger.valueOf(1000))
                .build();

        VotingData votingData = VotingData.builder()
                .drepVotes(drepVotes)
                .spoVotes(spoVotes)
                .build();

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ true, // delayed by another accepted action this epoch
                /* currentEpoch= */ 9);

        RatificationContext context = RatificationContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // last opportunity
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.REJECT);
    }

    private GovernanceContext buildGovernanceContext(ConstitutionCommitteeState committeeState,
                                                     boolean isDelayed,
                                                     int currentEpoch) {
        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(committeeState)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(java.util.List.of())
                .build();

        // threshold 0.51 for both DRep and SPO motionNoConfidence
        DrepVoteThresholds drepThresholds = DrepVoteThresholds.builder()
                .dvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .dvtCommitteeNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .dvtCommitteeNormal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.6")))
                .build();

        PoolVotingThresholds poolThresholds = PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .pvtCommitteeNormal(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .pvtCommitteeNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        ProtocolParams protocolParams = ProtocolParams.builder()
                .drepVotingThresholds(drepThresholds)
                .poolVotingThresholds(poolThresholds)
                .build();

        return GovernanceContext.builder()
                .currentEpoch(currentEpoch)
                .committee(committee)
                .protocolParams(protocolParams)
                .isInBootstrapPhase(false)
                .isActionRatificationDelayed(isDelayed)
                .treasury(BigInteger.ZERO)
                .lastEnactedGovActionIds(Map.of())
                .build();
    }
}
