package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GovernanceEvaluationServiceTest {

    private GovernanceEvaluationService service;
    private Map<GovActionType, RatificationEvaluator> evaluatorCache;
    private Map<GovActionType, RatificationEvaluator> originalEvaluators;

    @BeforeEach
    void setUp() {
        service = new GovernanceEvaluationService();
        evaluatorCache = accessEvaluatorCache();
        originalEvaluators = new EnumMap<>(GovActionType.class);
    }

    @AfterEach
    void tearDown() {
        originalEvaluators.forEach(evaluatorCache::put);
        originalEvaluators.clear();
    }

    @Test
    void evaluateGovernanceState_sortsResultsAndDropsSiblingsForDelayingAcceptance() {
        // Purpose: ensure delaying actions take priority and trigger drop logic for sibling proposals.
        VotingData votingData = VotingData.builder().build();

        GovActionId updateActionId = mock(GovActionId.class);
        GovActionId noConfidenceActionId = mock(GovActionId.class);
        GovActionId parameterActionId = mock(GovActionId.class);

        // Init proposals: compose a delaying update committee, a no-confidence sibling, and a lower priority parameter change.
        UpdateCommittee updateCommittee = mock(UpdateCommittee.class);
        when(updateCommittee.getType()).thenReturn(GovActionType.UPDATE_COMMITTEE);
        when(updateCommittee.getGovActionId()).thenReturn(null);

        NoConfidence noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        when(noConfidence.getGovActionId()).thenReturn(null);

        ParameterChangeAction parameterChange = mock(ParameterChangeAction.class);
        when(parameterChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(parameterChange.getGovActionId()).thenReturn(null);

        ProposalContext updateCommitteeProposal = ProposalContext.builder()
                .govAction(updateCommittee)
                .votingData(votingData)
                .govActionId(updateActionId)
                .maxAllowedVotingEpoch(8)
                .proposalSlot(30L)
                .build();

        ProposalContext parameterChangeProposal = ProposalContext.builder()
                .govAction(parameterChange)
                .votingData(votingData)
                .govActionId(parameterActionId)
                .maxAllowedVotingEpoch(9)
                .proposalSlot(10L)
                .build();

        ProposalContext noConfidenceProposal = ProposalContext.builder()
                .govAction(noConfidence)
                .votingData(votingData)
                .govActionId(noConfidenceActionId)
                .maxAllowedVotingEpoch(10)
                .proposalSlot(20L)
                .build();

        // Stub ratification: accept the delaying action, continue sibling, reject parameter change to exercise drop + expire paths.
        stubEvaluator(GovActionType.UPDATE_COMMITTEE,
                evaluatorFor(singletonOutcome(updateCommittee, RatificationResult.ACCEPT), RatificationResult.CONTINUE));
        stubEvaluator(GovActionType.NO_CONFIDENCE,
                evaluatorFor(singletonOutcome(noConfidence, RatificationResult.CONTINUE), RatificationResult.CONTINUE));
        stubEvaluator(GovActionType.PARAMETER_CHANGE_ACTION,
                evaluatorFor(singletonOutcome(parameterChange, RatificationResult.REJECT), RatificationResult.CONTINUE));

        GovernanceEvaluationInput input = GovernanceEvaluationInput.builder()
                .currentProposals(List.of(updateCommitteeProposal, parameterChangeProposal, noConfidenceProposal))
                .currentEpoch(5)
                .protocolParams(ProtocolParams.builder().build())
                .committee(ConstitutionCommittee.builder().state(ConstitutionCommitteeState.NORMAL).build())
                .isBootstrapPhase(false)
                .treasury(BigInteger.ZERO)
                .lastEnactedGovActionIds(Map.of())
                .build();

        GovernanceEvaluationResult result = service.evaluateGovernanceState(input);

        // Expect prioritisation: no-confidence evaluated first (same committee purpose) followed by delaying update, then parameter change.
        assertThat(result.getProposalResults())
                .extracting(proposalResult -> proposalResult.getProposal().getType())
                .containsExactly(
                        GovActionType.NO_CONFIDENCE,
                        GovActionType.UPDATE_COMMITTEE,
                        GovActionType.PARAMETER_CHANGE_ACTION
                );

        // Expect statuses determined by stubs and drop service triggered by rejection.
        assertThat(result.getProposalResults())
                .extracting(ProposalEvaluationResult::getStatus)
                .containsExactly(
                        RatificationResult.CONTINUE,
                        RatificationResult.ACCEPT,
                        RatificationResult.REJECT
                );

        // Delaying action accepted -> ratification should be marked as delayed.
        assertThat(result.isActionRatificationDelayed()).isTrue();

        Proposal expectedDroppedProposal = Proposal.builder()
                .type(GovActionType.NO_CONFIDENCE)
                .govActionId(noConfidenceActionId)
                .build();

        // Sibling of accepted delaying action should be dropped next epoch.
        assertThat(result.getProposalsToDropNext())
                .containsExactly(expectedDroppedProposal);
    }

    @Test
    void evaluateGovernanceState_ordersSamePriorityBySlotAndKeepsDelayFlagForNonDelayingActions() {
        // Purpose: ensure proposals with same priority are sorted by slot and non-delaying acceptance leaves delay flag false.
        VotingData votingData = VotingData.builder().build();

        GovActionId firstActionId = mock(GovActionId.class);
        GovActionId secondActionId = mock(GovActionId.class);

        ParameterChangeAction higherSlotAction = mock(ParameterChangeAction.class);
        when(higherSlotAction.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(higherSlotAction.getGovActionId()).thenReturn(null);

        ParameterChangeAction lowerSlotAction = mock(ParameterChangeAction.class);
        when(lowerSlotAction.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);
        when(lowerSlotAction.getGovActionId()).thenReturn(null);

        ProposalContext laterProposal = ProposalContext.builder()
                .govAction(higherSlotAction)
                .votingData(votingData)
                .govActionId(firstActionId)
                .maxAllowedVotingEpoch(8)
                .proposalSlot(50L)
                .build();

        ProposalContext earlierProposal = ProposalContext.builder()
                .govAction(lowerSlotAction)
                .votingData(votingData)
                .govActionId(secondActionId)
                .maxAllowedVotingEpoch(8)
                .proposalSlot(20L)
                .build();

        Map<GovAction, RatificationResult> outcomes = new IdentityHashMap<>();
        outcomes.put(higherSlotAction, RatificationResult.ACCEPT);
        outcomes.put(lowerSlotAction, RatificationResult.CONTINUE);

        // Stub both parameter change actions to return unique results per identity.
        stubEvaluator(GovActionType.PARAMETER_CHANGE_ACTION,
                evaluatorFor(outcomes, RatificationResult.CONTINUE));

        GovernanceEvaluationInput input = GovernanceEvaluationInput.builder()
                .currentProposals(List.of(laterProposal, earlierProposal))
                .currentEpoch(6)
                .protocolParams(ProtocolParams.builder().build())
                .committee(ConstitutionCommittee.builder().state(ConstitutionCommitteeState.NORMAL).build())
                .isBootstrapPhase(false)
                .treasury(BigInteger.TEN)
                .lastEnactedGovActionIds(Map.of())
                .build();

        GovernanceEvaluationResult result = service.evaluateGovernanceState(input);

        // Expect earlier slot first despite later appearing first in input list.
        assertThat(result.getProposalResults())
                .extracting(proposalResult -> proposalResult.getProposal().getGovActionId())
                .containsExactly(secondActionId, firstActionId);

        // Combined outcomes should match stubbed results.
        assertThat(result.getProposalResults())
                .extracting(ProposalEvaluationResult::getStatus)
                .containsExactly(RatificationResult.CONTINUE, RatificationResult.ACCEPT);

        // Non-delaying action accepted -> global delay flag remains false.
        assertThat(result.isActionRatificationDelayed()).isFalse();

        Proposal expectedDroppedProposal = Proposal.builder()
                .type(GovActionType.PARAMETER_CHANGE_ACTION)
                .govActionId(secondActionId)
                .build();

        // Only the continuing proposal should be dropped by ProposalDropService.
        assertThat(result.getProposalsToDropNext())
                .containsExactly(expectedDroppedProposal);
    }

    @Test
    void evaluateGovernanceState_propagatesDelayFlagToSubsequentEvaluations() {
        // Purpose: ensure evaluating a delaying proposal sets the flag consumed by later evaluations within the same epoch.
        VotingData votingData = VotingData.builder().build();

        GovActionId updateCommitteeActionId = mock(GovActionId.class);
        GovActionId ppChangeActionId = mock(GovActionId.class);

        // Init mix: delaying committee change and neutral info action tested for propagated context.
        UpdateCommittee updateCommittee = mock(UpdateCommittee.class);
        when(updateCommittee.getType()).thenReturn(GovActionType.UPDATE_COMMITTEE);
        when(updateCommittee.getGovActionId()).thenReturn(null);

        ParameterChangeAction parameterChange = mock(ParameterChangeAction.class);
        when(parameterChange.getType()).thenReturn(GovActionType.PARAMETER_CHANGE_ACTION);

        AtomicBoolean ppChangeContextWasDelayed = new AtomicBoolean(false);

        // Stub the update evaluator to return ACCEPT so the delay flag becomes true, capture flag visible inside param change evaluation.
        stubEvaluator(GovActionType.UPDATE_COMMITTEE, context -> RatificationResult.ACCEPT);
        stubEvaluator(GovActionType.PARAMETER_CHANGE_ACTION, context -> {
            ppChangeContextWasDelayed.set(context.getGovernanceContext().isActionRatificationDelayed());
            return RatificationResult.CONTINUE;
        });

        ProposalContext updateCommitteeProposal = ProposalContext.builder()
                .govAction(updateCommittee)
                .votingData(votingData)
                .govActionId(updateCommitteeActionId)
                .maxAllowedVotingEpoch(7)
                .proposalSlot(10L)
                .build();

        ProposalContext paramChangeProposal = ProposalContext.builder()
                .govAction(parameterChange)
                .votingData(votingData)
                .govActionId(ppChangeActionId)
                .maxAllowedVotingEpoch(7)
                .proposalSlot(40L)
                .build();

        GovernanceEvaluationInput input = GovernanceEvaluationInput.builder()
                .currentProposals(List.of(paramChangeProposal, updateCommitteeProposal))
                .currentEpoch(9)
                .protocolParams(ProtocolParams.builder().build())
                .committee(ConstitutionCommittee.builder().state(ConstitutionCommitteeState.NORMAL).build())
                .isBootstrapPhase(false)
                .treasury(BigInteger.ZERO)
                .lastEnactedGovActionIds(Map.of())
                .build();

        GovernanceEvaluationResult result = service.evaluateGovernanceState(input);

        // Expect prioritisation of delaying action ahead of param change action despite input order.
        assertThat(result.getProposalResults())
                .extracting(proposalResult -> proposalResult.getProposal().getType())
                .containsExactly(
                        GovActionType.UPDATE_COMMITTEE,
                        GovActionType.PARAMETER_CHANGE_ACTION
                );

        // Delay flag must stay true and be observable inside later evaluation.
        assertThat(result.isActionRatificationDelayed()).isTrue();
        assertThat(ppChangeContextWasDelayed.get()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private Map<GovActionType, RatificationEvaluator> accessEvaluatorCache() {
        try {
            Field field = RatificationEvaluatorFactory.class.getDeclaredField("EVALUATOR_CACHE");
            field.setAccessible(true);
            return (Map<GovActionType, RatificationEvaluator>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access evaluator cache", e);
        }
    }

    private void stubEvaluator(GovActionType type, RatificationEvaluator evaluator) {
        originalEvaluators.putIfAbsent(type, evaluatorCache.get(type));
        evaluatorCache.put(type, evaluator);
    }

    private RatificationEvaluator evaluatorFor(Map<GovAction, RatificationResult> assignments,
                                              RatificationResult defaultResult) {
        Map<GovAction, RatificationResult> identityAssignments = new IdentityHashMap<>(assignments);

        return context -> {
            GovAction action = context.getGovAction();
            return identityAssignments.getOrDefault(action, defaultResult);
        };
    }

    private Map<GovAction, RatificationResult> singletonOutcome(GovAction action, RatificationResult result) {
        Map<GovAction, RatificationResult> assignments = new IdentityHashMap<>();
        assignments.put(action, result);
        return assignments;
    }
}
