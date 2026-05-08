package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
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

class ParameterChangeRatificationEvaluatorTest {

    private final ParameterChangeRatificationEvaluator evaluator = new ParameterChangeRatificationEvaluator();

    /**
     * When committee is in NO_CONFIDENCE state, ParameterChange cannot be ratified
     */
    @Test
    void evaluate_returnsContinue_whenCommitteeIsInNoConfidence_becauseCommitteeVoteFails() {
        VotingData votingData = buildFullPassingVotesWithCommitteeYes();

        ParameterChangeAction paramChange = mock(ParameterChangeAction.class);
        when(paramChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(paramChange.getGovActionId()).thenReturn(null);
        when(paramChange.getProtocolParamUpdate()).thenReturn(ProtocolParamUpdate.builder().minFeeA(10).build());

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(paramChange)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        // Committee vote fails due to NO_CONFIDENCE state → CONTINUE (not accepted)
        assertThat(result).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsAccept_whenCommitteeIsNormal_andAllVotesPass() {
        VotingData votingData = buildFullPassingVotesWithCommitteeYes();

        ParameterChangeAction paramChange = mock(ParameterChangeAction.class);

        when(paramChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(paramChange.getGovActionId()).thenReturn(null);
        when(paramChange.getProtocolParamUpdate()).thenReturn(ProtocolParamUpdate.builder().minFeeA(10).build());

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ false,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(paramChange)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.ACCEPT);
    }

    @Test
    void evaluate_returnsContinue_whenDelayed_andNormalState() {
        VotingData votingData = buildFullPassingVotesWithCommitteeYes();

        ParameterChangeAction paramChange = mock(ParameterChangeAction.class);
        when(paramChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(paramChange.getGovActionId()).thenReturn(null);
        when(paramChange.getProtocolParamUpdate()).thenReturn(ProtocolParamUpdate.builder().minFeeA(10).build());


        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL,
                /* isDelayed= */ true,
                /* currentEpoch= */ 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(paramChange)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        assertThat(evaluator.evaluate(context)).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsReject_whenOutOfLifecycle() {
        ParameterChangeAction paramChange = mock(ParameterChangeAction.class);
        when(paramChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(paramChange.getGovActionId()).thenReturn(null);
        when(paramChange.getProtocolParamUpdate()).thenReturn(ProtocolParamUpdate.builder().minFeeA(10).build());

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL, false, 15);

        RatificationContext context = RatificationContext.builder()
                .govAction(paramChange)
                .votingData(buildFullPassingVotesWithCommitteeYes())
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        assertThat(evaluator.evaluate(context)).isEqualTo(RatificationResult.REJECT);
    }

    private VotingData buildFullPassingVotesWithCommitteeYes() {
        return VotingData.builder()
                .committeeVotes(VotingData.CommitteeVotes.builder()
                        .votes(Map.of(
                                "hot1", com.bloxbean.cardano.yaci.core.model.governance.Vote.YES,
                                "hot2", com.bloxbean.cardano.yaci.core.model.governance.Vote.YES))
                        .build())
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
                .dvtPPNetworkGroup(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .dvtPPEconomicGroup(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .dvtPPTechnicalGroup(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .dvtPPGovGroup(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        PoolVotingThresholds poolThresholds = PoolVotingThresholds.builder()
                .pvtPPSecurityGroup(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
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
