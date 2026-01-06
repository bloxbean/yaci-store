package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.domain.ProposalStatusCapturedEvent;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedGovernanceData;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.ProposalStateService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingStatsService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.GovernanceEvaluationInputMapper;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationInput;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationResult;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalStateProcessorTest {

    @Mock
    private ProposalStateService proposalStateService;
    @Mock
    private VotingStatsService votingStatsService;
    @Mock
    private ProposalStatusMapper proposalStatusMapper;
    @Mock
    private GovernanceEvaluationInputMapper governanceEvaluationInputMapper;
    @Mock
    private GovActionProposalStatusStorage govActionProposalStatusStorage;
    @Mock
    private EraService eraService;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private DRepDistService dRepDistService;
    @Mock
    private GovernanceEvaluationService governanceEvaluationService;

    @Mock
    private AdaPotJobStorage adaPotJobStorage;

    private ProposalStateProcessor proposalStateProcessor;

    @BeforeEach
    void setUp() {
        proposalStateProcessor = new ProposalStateProcessor(
                proposalStateService,
                dRepDistService,
                votingStatsService,
                eraService,
                govActionProposalStatusStorage,
                adaPotJobStorage,
                governanceEvaluationInputMapper,
                proposalStatusMapper,
                publisher) {
            @Override
            GovernanceEvaluationService createGovernanceEvaluationService() {
                return governanceEvaluationService;
            }
        };
    }

    @Test
    void testHandleProposalState_whenConwayEra_andDataExists() {
        int epoch = 100;
        int currentEpoch = epoch + 1;
        long slot = 2000L;
        StakeSnapshotTakenEvent event = new StakeSnapshotTakenEvent(epoch, slot);

        when(eraService.getEraForEpoch(epoch)).thenReturn(Era.Conway);

        AggregatedGovernanceData aggregatedData = mock(AggregatedGovernanceData.class);
        when(proposalStateService.collectGovernanceData(currentEpoch)).thenReturn(aggregatedData);

        GovernanceEvaluationInput input = mock(GovernanceEvaluationInput.class);
        when(governanceEvaluationInputMapper.toGovernanceEvaluationInput(aggregatedData)).thenReturn(input);

        GovernanceEvaluationResult result = mock(GovernanceEvaluationResult.class);
        when(governanceEvaluationService.evaluateGovernanceState(input)).thenReturn(result);

        Map<String, Object> statsMap = Collections.emptyMap();
        when(votingStatsService.computeVotingStats(aggregatedData)).thenReturn((Map) statsMap);

        GovActionProposalStatus status = new GovActionProposalStatus();
        List<GovActionProposalStatus> statusList = List.of(status);
        when(proposalStatusMapper.mapToProposalStatus(eq(result), eq(currentEpoch), any())).thenReturn(statusList);

        proposalStateProcessor.handleProposalState(event);

        verify(dRepDistService).takeStakeSnapshot(currentEpoch);
        verify(proposalStateService).collectGovernanceData(currentEpoch);
        verify(govActionProposalStatusStorage).deleteByEpoch(currentEpoch);
        verify(govActionProposalStatusStorage).saveAll(statusList);
        verify(publisher, times(1)).publishEvent(any(ProposalStatusCapturedEvent.class));
    }

    @Test
    void testHandleProposalState_whenConwayEra_andNoData() {
        int epoch = 100;
        int currentEpoch = epoch + 1;
        long slot = 2000L;
        StakeSnapshotTakenEvent event = new StakeSnapshotTakenEvent(epoch, slot);

        when(eraService.getEraForEpoch(epoch)).thenReturn(Era.Conway);
        when(proposalStateService.collectGovernanceData(currentEpoch)).thenReturn(null);

        proposalStateProcessor.handleProposalState(event);

        verify(dRepDistService).takeStakeSnapshot(currentEpoch);
        verify(proposalStateService).collectGovernanceData(currentEpoch);
        verify(govActionProposalStatusStorage).deleteByEpoch(currentEpoch);
        verify(govActionProposalStatusStorage, never()).saveAll(any());
        verify(publisher, times(1)).publishEvent(any(ProposalStatusCapturedEvent.class));
    }

    @Test
    void testHandleProposalState_whenPreConwayEra() {
        int epoch = 50;
        long slot = 1000L;
        StakeSnapshotTakenEvent event = new StakeSnapshotTakenEvent(epoch, slot);

        when(eraService.getEraForEpoch(epoch)).thenReturn(Era.Shelley);

        proposalStateProcessor.handleProposalState(event);

        verifyNoInteractions(dRepDistService);
        verifyNoInteractions(proposalStateService);
    }

    @Test
    void testGetProposalStatuses_whenDataExists() {
        int epoch = 100;

        AggregatedGovernanceData aggregatedData = mock(AggregatedGovernanceData.class);
        when(proposalStateService.collectGovernanceData(epoch)).thenReturn(aggregatedData);

        GovernanceEvaluationInput input = mock(GovernanceEvaluationInput.class);
        when(governanceEvaluationInputMapper.toGovernanceEvaluationInput(aggregatedData)).thenReturn(input);

        GovernanceEvaluationResult result = mock(GovernanceEvaluationResult.class);
        when(governanceEvaluationService.evaluateGovernanceState(input)).thenReturn(result);

        Map<String, Object> statsMap = Collections.emptyMap();
        when(votingStatsService.computeVotingStats(aggregatedData)).thenReturn((Map) statsMap);

        GovActionProposalStatus status = new GovActionProposalStatus();
        List<GovActionProposalStatus> expectedList = List.of(status);
        when(proposalStatusMapper.mapToProposalStatus(eq(result), eq(epoch), any())).thenReturn(expectedList);

        List<GovActionProposalStatus> actualList = proposalStateProcessor.getProposalStatuses(epoch);

        assertThat(actualList).isEqualTo(expectedList);
    }

    @Test
    void testGetProposalStatuses_empty() {
        int epoch = 100;
        when(proposalStateService.collectGovernanceData(epoch)).thenReturn(null);

        List<GovActionProposalStatus> actualList = proposalStateProcessor.getProposalStatuses(epoch);

        assertThat(actualList).isEmpty();
    }
}
