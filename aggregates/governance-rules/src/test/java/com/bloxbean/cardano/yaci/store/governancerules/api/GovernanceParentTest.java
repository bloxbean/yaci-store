package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceParentTest {
    private final GovernanceEvaluationService service = new GovernanceEvaluationService();

    @Test
    void childRemainsPendingUntilItsParentIsEnacted() {
        var rootAction = ParameterChangeAction.builder()
                .protocolParamUpdate(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()).build();
        var pendingParent = proposal(1, rootAction, 0);
        var childAction = ParameterChangeAction.builder().govActionId(pendingParent.getGovActionId())
                .protocolParamUpdate(ProtocolParamUpdate.builder().poolDeposit(BigInteger.TEN).build()).build();
        var child = proposal(2, childAction, 100);
        var result = evaluate(List.of(pendingParent, child), 100, null, "0.51");
        assertThat(result).containsExactly(RatificationResult.CONTINUE, RatificationResult.CONTINUE);
        assertThat(evaluate(List.of(child), 100, id(99), "0.51"))
                .containsExactly(RatificationResult.CONTINUE);
        assertThat(evaluate(List.of(child), 100, pendingParent.getGovActionId(), "0.51"))
                .containsExactly(RatificationResult.ACCEPT);
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

    private static GovActionId id(int n) { return new GovActionId("%064x".formatted(n), 0); }
    private static UnitInterval ratio(String value) { return UnitIntervalUtil.decimalToUnitInterval(new BigDecimal(value)); }
}
