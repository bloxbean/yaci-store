package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.domain.*;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.domain.*;
import org.junit.jupiter.api.Test;
import java.math.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class GovernanceStateThreadingTest {
    private final GovernanceEvaluationService service = new GovernanceEvaluationService();
    private final GovActionId parent = id(0);

    @Test
    void withdrawalsUseRemainingTreasury() {
        var first = proposal(1, withdrawal(), 100);
        var second = proposal(2, withdrawal(), 100);
        var actual = evaluate(List.of(first, second), 100, parent, "0.51");
        assertThat(actual).containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        var secondWithRemainingTreasury = evaluate(List.of(second), 30, parent, "0.51");
        assertThat(secondWithRemainingTreasury).containsExactly(RatificationResult.CONTINUE);
    }

    @Test
    void parameterSiblingsCannotBothRatify() {
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().keyDeposit(BigInteger.valueOf(20)).build()), 100);
        var second = proposal(2, parameter(ProtocolParamUpdate.builder().keyDeposit(BigInteger.valueOf(30)).build()), 100);
        var actual = evaluate(List.of(first, second), 100, parent, "0.51");
        assertThat(actual).containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        var secondWithAdvancedParent = evaluate(List.of(second), 100, first.getGovActionId(), "0.51");
        assertThat(secondWithAdvancedParent).containsExactly(RatificationResult.CONTINUE);
    }

    @Test
    void laterWithdrawalUsesUpdatedVotingThreshold() {
        var thresholds = com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds.builder()
                .dvtMotionNoConfidence(ratio("0.51")).dvtCommitteeNormal(ratio("0.51"))
                .dvtCommitteeNoConfidence(ratio("0.51")).dvtUpdateToConstitution(ratio("0.51"))
                .dvtHardForkInitiation(ratio("0.51")).dvtPPNetworkGroup(ratio("0.51"))
                .dvtPPEconomicGroup(ratio("0.51")).dvtPPTechnicalGroup(ratio("0.51"))
                .dvtPPGovGroup(ratio("0.51")).dvtTreasuryWithdrawal(ratio("0.90")).build();
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().drepVotingThresholds(thresholds).build()), 100);
        var second = proposal(2, withdrawal(), 60);
        var actual = evaluate(List.of(first, second), 100, parent, "0.51");
        assertThat(actual).containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        var secondWithUpdatedThreshold = evaluate(List.of(second), 100, first.getGovActionId(), "0.90");
        assertThat(secondWithUpdatedThreshold).containsExactly(RatificationResult.CONTINUE);
    }

    @Test
    void parameterChildCanRatifyAfterItsParent() {
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()), 100);
        var child = proposal(2, ParameterChangeAction.builder().govActionId(first.getGovActionId())
                .protocolParamUpdate(ProtocolParamUpdate.builder().poolDeposit(BigInteger.TEN).build()).build(), 100);
        assertThat(evaluate(List.of(child, first), 100, parent, "0.51"))
                .containsExactly(RatificationResult.ACCEPT, RatificationResult.ACCEPT);
    }

    @Test
    void rejectedAndPendingWithdrawalsDoNotConsumeTreasury() {
        var rejected = proposal(1, withdrawal(), 0);
        var accepted = proposal(2, withdrawal(), 100);
        assertThat(evaluate(List.of(rejected, accepted), 100, parent, "0.51"))
                .containsExactly(RatificationResult.CONTINUE, RatificationResult.ACCEPT);
        var tooLarge = proposal(1, TreasuryWithdrawalsAction.builder()
                .withdrawals(Map.of("e1" + "11".repeat(28), BigInteger.valueOf(101))).build(), 100);
        assertThat(evaluate(List.of(tooLarge, accepted), 100, parent, "0.51"))
                .containsExactly(RatificationResult.CONTINUE, RatificationResult.ACCEPT);
    }

    @Test
    void withdrawalsCanExactlyExhaustTreasury() {
        var first = proposal(1, withdrawal(), 100);
        var second = proposal(2, TreasuryWithdrawalsAction.builder()
                .withdrawals(Map.of("e1" + "11".repeat(28), BigInteger.valueOf(30))).build(), 100);
        assertThat(evaluate(List.of(first, second), 100, parent, "0.51"))
                .containsExactly(RatificationResult.ACCEPT, RatificationResult.ACCEPT);
    }

    @Test
    void pendingParameterChangeDoesNotAdvanceParentOrParameters() {
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().committeeMinSize(2).build()), 0);
        var second = proposal(2, parameter(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()), 100);
        assertThat(evaluate(List.of(first, second), 100, parent, "0.51"))
                .containsExactly(RatificationResult.CONTINUE, RatificationResult.ACCEPT);
    }

    @Test
    void committeeSizeChangeAppliesToLaterWithdrawal() {
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().committeeMinSize(2).build()), 100);
        var second = proposal(2, withdrawal(), 100);
        var input = input(List.of(first, second), 100, parent, "0.51");
        assertThat(statuses(input)).containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        // Evaluating the same input again must give the same result and leave its snapshot intact.
        assertThat(statuses(input)).containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        assertThat(input.getProtocolParams().getCommitteeMinSize()).isEqualTo(1);
        assertThat(input.getLastEnactedGovActionIds()).containsEntry(ProposalType.P_PARAM_UPDATE, parent);
        assertThat(input.getTreasury()).isEqualTo(BigInteger.valueOf(100));
    }

    @Test
    void firstParameterChangeAdvancesAnInitiallyEmptyParent() {
        var action = ParameterChangeAction.builder()
                .protocolParamUpdate(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()).build();
        assertThat(evaluate(List.of(proposal(1, action, 100), proposal(2, action, 100)), 100, null, "0.51"))
                .containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
    }

    @Test
    void poolThresholdChangeAppliesToLaterParameterChild() {
        var thresholds = com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds.builder()
                .pvtMotionNoConfidence(ratio("0.51")).pvtCommitteeNormal(ratio("0.51"))
                .pvtCommitteeNoConfidence(ratio("0.51")).pvtHardForkInitiation(ratio("0.51"))
                .pvtPPSecurityGroup(ratio("0.90")).build();
        var first = proposal(1, parameter(ProtocolParamUpdate.builder().poolVotingThresholds(thresholds).build()), 100);
        var child = proposal(2, ParameterChangeAction.builder().govActionId(first.getGovActionId())
                .protocolParamUpdate(ProtocolParamUpdate.builder().minFeeA(50).build()).build(), 100, 60);
        assertThat(evaluate(List.of(first, child), 100, parent, "0.51"))
                .containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE);
        assertThat(evaluate(List.of(child), 100, first.getGovActionId(), "0.51"))
                .containsExactly(RatificationResult.ACCEPT);
    }

    @Test
    void delayingActionLeavesLaterProposalsPendingOrExpired() {
        var first = proposal(1, NoConfidence.builder().build(), 100);
        var pending = proposal(2, withdrawal(), 100);
        var expired = ProposalContext.builder().govActionId(id(3)).govAction(withdrawal())
                .proposalSlot(3L).maxAllowedVotingEpoch(4).votingData(pending.getVotingData()).build();
        assertThat(evaluate(List.of(first, pending, expired), 100, parent, "0.51"))
                .containsExactly(RatificationResult.ACCEPT, RatificationResult.CONTINUE, RatificationResult.REJECT);
    }

    private List<RatificationResult> evaluate(List<ProposalContext> proposals, int treasury,
                                             GovActionId lastParent, String withdrawalThreshold) {
        return statuses(input(proposals, treasury, lastParent, withdrawalThreshold));
    }

    private List<RatificationResult> statuses(GovernanceEvaluationInput input) {
        return service.evaluateGovernanceState(input).getProposalResults().stream()
                .map(ProposalEvaluationResult::getStatus).toList();
    }

    private GovernanceEvaluationInput input(List<ProposalContext> proposals, int treasury,
                                             GovActionId lastParent, String withdrawalThreshold) {
        var params = ProtocolParams.builder().committeeMinSize(1)
                .drepVotingThresholds(DrepVoteThresholds.builder()
                        .dvtMotionNoConfidence(ratio("0.51")).dvtPPEconomicGroup(ratio("0.51")).dvtPPGovGroup(ratio("0.51"))
                        .dvtTreasuryWithdrawal(ratio(withdrawalThreshold)).build())
                .poolVotingThresholds(PoolVotingThresholds.builder().pvtMotionNoConfidence(ratio("0.51"))
                        .pvtPPSecurityGroup(ratio("0.51")).build())
                .build();
        var committee = ConstitutionCommittee.builder().state(ConstitutionCommitteeState.NORMAL)
                .threshold(ratio("0.51")).members(List.of(
                        CommitteeMember.builder().coldKey("cold").hotKey("hot").build())).build();
        return GovernanceEvaluationInput.builder()
                        .currentEpoch(5).currentProposals(proposals).protocolParams(params).committee(committee)
                        .treasury(BigInteger.valueOf(treasury)).isBootstrapPhase(false)
                        .lastEnactedGovActionIds(lastParent == null ? Map.of() : Map.of(ProposalType.P_PARAM_UPDATE, lastParent)).build();
    }

    private ProposalContext proposal(int number, GovAction action, int yes) {
        return proposal(number, action, yes, 100);
    }

    private ProposalContext proposal(int number, GovAction action, int yes, int poolYes) {
        return ProposalContext.builder().govActionId(id(number)).govAction(action).proposalSlot((long) number)
                .maxAllowedVotingEpoch(8).votingData(VotingData.builder()
                        .committeeVotes(VotingData.CommitteeVotes.builder().votes(Map.of("hot", Vote.YES)).build())
                        .spoVotes(VotingData.SPOVotes.builder().yesVoteStake(BigInteger.valueOf(poolYes))
                                .totalStake(BigInteger.valueOf(100)).doNotVoteStake(BigInteger.valueOf(100 - poolYes))
                                .abstainVoteStake(BigInteger.ZERO).delegateToAutoAbstainDRepStake(BigInteger.ZERO)
                                .delegateToNoConfidenceDRepStake(BigInteger.ZERO).build())
                        .drepVotes(VotingData.DRepVotes.builder().yesVoteStake(BigInteger.valueOf(yes))
                                .noVoteStake(BigInteger.valueOf(100 - yes)).noConfidenceStake(BigInteger.ZERO)
                                .doNotVoteStake(BigInteger.ZERO).build()).build()).build();
    }

    private TreasuryWithdrawalsAction withdrawal() {
        return TreasuryWithdrawalsAction.builder().withdrawals(Map.of("e1" + "11".repeat(28), BigInteger.valueOf(70))).build();
    }
    private ParameterChangeAction parameter(ProtocolParamUpdate update) {
        return ParameterChangeAction.builder().govActionId(parent).protocolParamUpdate(update).build();
    }
    private static GovActionId id(int n) { return new GovActionId("%064x".formatted(n), 0); }
    private static UnitInterval ratio(String value) { return UnitIntervalUtil.decimalToUnitInterval(new BigDecimal(value)); }
}
