package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.domain.ProposalStatusCapturedEvent;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.ProposalStateService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingStatsService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.GovernanceEvaluationInputMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationInput;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationResult;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration.STORE_GOVERNANCEAGGR_ENABLED;

@Component
@EnableIf(value = STORE_GOVERNANCEAGGR_ENABLED, defaultValue = false)
@RequiredArgsConstructor
@Slf4j
public class ProposalStateProcessor {
    
    private final ProposalStateService proposalStateService;
    private final DRepDistService dRepDistService;
    private final VotingStatsService votingStatsService;
    private final EraService eraService;
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final AdaPotJobStorage adaPotJobStorage;

    private final GovernanceEvaluationInputMapper governanceEvaluationInputMapper;
    private final ProposalStatusMapper proposalStatusMapper;

    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleProposalState(StakeSnapshotTakenEvent stakeSnapshotTakenEvent) {
        if (eraService.getEraForEpoch(stakeSnapshotTakenEvent.getEpoch()).getValue() < Era.Conway.getValue()) {
            return;
        }
        
        int epoch = stakeSnapshotTakenEvent.getEpoch();
        int currentEpoch = epoch + 1;
        
        log.info("Processing proposal status for epoch: {}", currentEpoch);

        try {
            // Delete existing records
            govActionProposalStatusStorage.deleteByEpoch(currentEpoch);

            // Take DRep distribution snapshot
            takeDRepDistrSnapshot(currentEpoch);

            GovernanceEvaluationService governanceEvaluationService = new GovernanceEvaluationService();

            // Collect aggregated governance data (proposals, votes, stake snapshots) for evaluation and voting stats computation
            var aggregatorGovernanceData = proposalStateService.collectGovernanceData(currentEpoch);

            if (aggregatorGovernanceData == null) {
                log.info("No proposals found for evaluation in epoch: {}", currentEpoch);
                return;
            }

            // convert to governance evaluation input
            GovernanceEvaluationInput input = governanceEvaluationInputMapper.toGovernanceEvaluationInput(aggregatorGovernanceData);

            // evaluate proposal status
            GovernanceEvaluationResult result = governanceEvaluationService.evaluateGovernanceState(input);

            // compute voting stats
            var statsMap = votingStatsService.computeVotingStats(aggregatorGovernanceData);

            // map to GovActionProposalStatus and save
            List<GovActionProposalStatus> statusList = proposalStatusMapper.mapToProposalStatus(result, currentEpoch, statsMap);
            
            if (!statusList.isEmpty()) {
                govActionProposalStatusStorage.saveAll(statusList);
            }
            
            log.info("Processed {} proposal statuses for epoch {}", statusList.size(), currentEpoch);
            
            publisher.publishEvent(new ProposalStatusCapturedEvent(currentEpoch, stakeSnapshotTakenEvent.getSlot()));
            
        } catch (Exception e) {
            log.error("Error processing proposal status for epoch {}", currentEpoch, e);
            throw e;
        }
    }

    private void takeDRepDistrSnapshot(int epoch) {
        var start = Instant.now();
        dRepDistService.takeStakeSnapshot(epoch);
        var end = Instant.now();

        Optional<AdaPotJob> adaPotJobOpt = adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch);
        if (adaPotJobOpt.isPresent()) {
            var job = adaPotJobOpt.get();
            job.setDrepDistrSnapshotTime(end.toEpochMilli() - start.toEpochMilli());

            adaPotJobStorage.save(job);
        }
    }
}
