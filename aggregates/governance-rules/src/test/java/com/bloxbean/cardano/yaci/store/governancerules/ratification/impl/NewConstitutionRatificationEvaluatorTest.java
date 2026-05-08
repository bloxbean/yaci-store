package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution;
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

class NewConstitutionRatificationEvaluatorTest {

    private final NewConstitutionRatificationEvaluator evaluator = new NewConstitutionRatificationEvaluator();

    /**
     * NewConstitution cannot be ratified in NO_CONFIDENCE state
     */
    @Test
    void evaluate_returnsContinue_whenCommitteeIsInNoConfidence_becauseCommitteeVoteFails() {
        VotingData votingData = buildFullPassingVotes();

        NewConstitution newConstitution = mock(NewConstitution.class);
        when(newConstitution.getType()).thenReturn(GovActionType.NEW_CONSTITUTION);
        when(newConstitution.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NO_CONFIDENCE, false, 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(newConstitution)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsAccept_whenCommitteeIsNormal_andAllVotesPass() {
        VotingData votingData = buildFullPassingVotes();

        NewConstitution newConstitution = mock(NewConstitution.class);
        when(newConstitution.getType()).thenReturn(GovActionType.NEW_CONSTITUTION);
        when(newConstitution.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL, false, 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(newConstitution)
                .votingData(votingData)
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        RatificationResult result = evaluator.evaluate(context);

        assertThat(result).isEqualTo(RatificationResult.ACCEPT);
    }

    @Test
    void evaluate_returnsContinue_whenDelayed_andNormalState() {
        NewConstitution newConstitution = mock(NewConstitution.class);
        when(newConstitution.getType()).thenReturn(GovActionType.NEW_CONSTITUTION);
        when(newConstitution.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL, true, 5);

        RatificationContext context = RatificationContext.builder()
                .govAction(newConstitution)
                .votingData(buildFullPassingVotes())
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8)
                .build();

        assertThat(evaluator.evaluate(context)).isEqualTo(RatificationResult.CONTINUE);
    }

    @Test
    void evaluate_returnsReject_whenOutOfLifecycle() {
        NewConstitution newConstitution = mock(NewConstitution.class);
        when(newConstitution.getType()).thenReturn(GovActionType.NEW_CONSTITUTION);
        when(newConstitution.getGovActionId()).thenReturn(null);

        GovernanceContext governanceContext = buildGovernanceContext(
                ConstitutionCommitteeState.NORMAL, false, 15);

        RatificationContext context = RatificationContext.builder()
                .govAction(newConstitution)
                .votingData(buildFullPassingVotes())
                .governanceContext(governanceContext)
                .maxAllowedVotingEpoch(8) // 15 - 8 = 7 > 1 → expired
                .build();

        assertThat(evaluator.evaluate(context)).isEqualTo(RatificationResult.REJECT);
    }

    private VotingData buildFullPassingVotes() {
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
                .dvtUpdateToConstitution(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51")))
                .build();

        ProtocolParams protocolParams = ProtocolParams.builder()
                .drepVotingThresholds(drepThresholds)
                .poolVotingThresholds(PoolVotingThresholds.builder().build())
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
