package com.bloxbean.cardano.yaci.store.governanceaggr.client;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import com.bloxbean.cardano.yaci.store.governancerules.api.*;
import com.bloxbean.cardano.yaci.store.governancerules.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ProposalStateClientImplTest {
    private final GovActionProposalStatusStorage statuses = mock(GovActionProposalStatusStorage.class);
    private final GovActionProposalStorage proposals = mock(GovActionProposalStorage.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProposalStateClientImpl client = new ProposalStateClientImpl(statuses, proposals, new ProposalMapper(objectMapper));

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void returnsChainTipAndAllowsItsSuccessorNextEpoch(boolean reverse) {
        var a = new GovActionId("ff".repeat(32), 0);
        var b = new GovActionId("00".repeat(32), 1);
        when(statuses.findLastEnactedProposals(GovActionType.PARAMETER_CHANGE_ACTION, 28)).thenReturn(List.of(
                GovActionProposalStatus.builder().govActionTxHash(a.getTransactionId()).govActionIndex(0).epoch(27).build(),
                GovActionProposalStatus.builder().govActionTxHash(b.getTransactionId()).govActionIndex(1).epoch(27).build()));
        var parent = stored(a, null);
        var child = stored(b, a);
        when(proposals.findByGovActionIds(anyList())).thenReturn(reverse ? List.of(child, parent) : List.of(parent, child));

        var tip = client.getLastEnactedProposal(GovActionType.PARAMETER_CHANGE_ACTION, 28).orElseThrow();
        var tipId = new GovActionId(tip.getTxHash(), tip.getIndex());
        assertThat(tipId).isEqualTo(b);

        var threshold = UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.51"));
        var successor = ProposalContext.builder().govActionId(new GovActionId("22".repeat(32), 0))
                .govAction(ParameterChangeAction.builder().govActionId(b)
                        .protocolParamUpdate(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()).build())
                .proposalSlot(101L).maxAllowedVotingEpoch(30)
                .votingData(VotingData.builder()
                        .committeeVotes(VotingData.CommitteeVotes.builder().votes(Map.of("hot", Vote.YES)).build())
                        .drepVotes(VotingData.DRepVotes.builder().yesVoteStake(BigInteger.TEN)
                                .noVoteStake(BigInteger.ZERO).noConfidenceStake(BigInteger.ZERO)
                                .doNotVoteStake(BigInteger.ZERO).build()).build()).build();
        var result = new GovernanceEvaluationService().evaluateGovernanceState(GovernanceEvaluationInput.builder()
                .currentEpoch(28).currentProposals(List.of(successor))
                .protocolParams(ProtocolParams.builder().committeeMinSize(1)
                        .drepVotingThresholds(DrepVoteThresholds.builder().dvtPPEconomicGroup(threshold).build()).build())
                .committee(ConstitutionCommittee.builder().state(ConstitutionCommitteeState.NORMAL).threshold(threshold)
                        .members(List.of(CommitteeMember.builder().coldKey("cold").hotKey("hot").build())).build())
                .lastEnactedGovActionIds(Map.of(ProposalType.P_PARAM_UPDATE, tipId)).build());
        assertThat(result.getProposalResults()).extracting(ProposalEvaluationResult::getStatus)
                .containsExactly(RatificationResult.ACCEPT);
    }

    @Test
    void failsWhenARatifiedProposalCannotBeRead() {
        var a = new GovActionId("ff".repeat(32), 0);
        var b = new GovActionId("00".repeat(32), 1);
        when(statuses.findLastEnactedProposals(GovActionType.PARAMETER_CHANGE_ACTION, 28)).thenReturn(List.of(
                GovActionProposalStatus.builder().govActionTxHash(a.getTransactionId()).govActionIndex(0).epoch(27).build(),
                GovActionProposalStatus.builder().govActionTxHash(b.getTransactionId()).govActionIndex(1).epoch(27).build()));
        when(proposals.findByGovActionIds(anyList())).thenReturn(List.of(stored(a, null)));

        assertThatThrownBy(() -> client.getLastEnactedProposal(GovActionType.PARAMETER_CHANGE_ACTION, 28))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(b.getTransactionId());
    }

    @Test
    void returnsEmptyWhenNoActionHasRatified() {
        when(statuses.findLastEnactedProposals(GovActionType.PARAMETER_CHANGE_ACTION, 28)).thenReturn(List.of());
        assertThat(client.getLastEnactedProposal(GovActionType.PARAMETER_CHANGE_ACTION, 28)).isEmpty();
        verifyNoInteractions(proposals);
    }

    private GovActionProposal stored(GovActionId id, GovActionId parent) {
        var action = ParameterChangeAction.builder().govActionId(parent)
                .protocolParamUpdate(ProtocolParamUpdate.builder().keyDeposit(BigInteger.TEN).build()).build();
        return GovActionProposal.builder().txHash(id.getTransactionId()).index(id.getGov_action_index())
                .slot(100L).epoch(26).type(GovActionType.PARAMETER_CHANGE_ACTION)
                .details(objectMapper.valueToTree(action)).build();
    }
}
