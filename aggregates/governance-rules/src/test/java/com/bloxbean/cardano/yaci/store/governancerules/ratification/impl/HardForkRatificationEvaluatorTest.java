package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.HardForkInitiationAction;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HardForkRatificationEvaluatorTest {

    private final HardForkRatificationEvaluator evaluator = new HardForkRatificationEvaluator();

    /**
     * When committee is in NO_CONFIDENCE state, HardFork  cannot be ratified
     */
    @Test
    void evaluate_returnsContinue_whenCommitteeIsInNoConfidence_evenIfAllVotesPass() {
        // All member votes YES but committee state is NO_CONFIDENCE → committee vote
        // fails
        VotingData votingData = buildFullPassingVotes(/* withCommitteeYes= */ true);

        HardForkInitiationAction hardFork = mock(HardForkInitiationAction.class);
        when(hardFork.getType()).thenReturn(GovActionType.HARD_FORK_INITIATION_ACTION);
        when(hardFork.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(hardFork)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        // isAccepted == false (committee NOT_PASS_THRESHOLD) → CONTINUE
        assertThat(result).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsAccept_whenCommitteeIsNormal_andAllVotesPass() {
        VotingData votingData = buildFullPassingVotes(/* withCommitteeYes= */ true);

        HardForkInitiationAction hardFork = mock(HardForkInitiationAction.class);
        when(hardFork.getType()).thenReturn(GovActionType.HARD_FORK_INITIATION_ACTION);
        when(hardFork.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(hardFork)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.ACCEPT);
    }

    @Test
    void evaluate_returnsContinue_whenDelayed_andNormalState() {
        VotingData votingData = buildFullPassingVotes(true);

        HardForkInitiationAction hardFork = mock(HardForkInitiationAction.class);
        when(hardFork.getType()).thenReturn(GovActionType.HARD_FORK_INITIATION_ACTION);
        when(hardFork.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ true, // another action was ratified this epoch
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(hardFork)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsReject_whenOutOfLifecycle() {
        VotingData votingData = buildFullPassingVotes(true);

        HardForkInitiationAction hardFork = mock(HardForkInitiationAction.class);
        when(hardFork.getType()).thenReturn(GovActionType.HARD_FORK_INITIATION_ACTION);
        when(hardFork.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL, false, 15);

        RatificationContext context = RatificationContext.builder()
                .govAction(hardFork)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // 15 - 8 = 7 > 1 → expired
                .build();

        assertThat(evaluator.evaluate(context)).isEqualTo(RatificationResult.REJECT);
    }

    /**
     * Builds voting data with 100% yes for DRep and SPO.
     */
    private VotingData buildFullPassingVotes(boolean withCommitteeYes) {
        Map<String, com.bloxbean.cardano.yaci.core.model.governance.Vote> committeeVotes = withCommitteeYes
                ? Map.of("hot1", com.bloxbean.cardano.yaci.core.model.governance.Vote.YES,
                "hot2", com.bloxbean.cardano.yaci.core.model.governance.Vote.YES)
                : Map.of();

        return VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder().votes(committeeVotes).build())
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
    }

    private GovernanceContext buildGovernanceContext(ConstitutionCommitteeState committeeState,
                                                     boolean isDelayed,
                                                     int currentEpoch) {
        List<CommitteeMember> members = List.of(
                CommitteeMember.builder().coldKey("cold1").hotKey("hot1").build(),
                CommitteeMember.builder().coldKey("cold2").hotKey("hot2").build());

        ConstitutionCommittee committee = ConstitutionCommittee.builder()
                .state(committeeState)
                .threshold(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .members(members)
                .build();

        DrepVoteThresholds drepThresholds = DrepVoteThresholds.builder()
                .dvtHardForkInitiation(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        PoolVotingThresholds poolThresholds = PoolVotingThresholds.builder()
                .pvtHardForkInitiation(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
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
