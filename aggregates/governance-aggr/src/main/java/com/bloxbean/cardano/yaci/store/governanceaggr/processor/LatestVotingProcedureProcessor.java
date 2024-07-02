package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.LatestVotingProcedureService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatestVotingProcedureProcessor {
    private final LatestVotingProcedureStorage latestVotingProcedureStorage;
    private final LatestVotingProcedureStorageReader latestVotingProcedureStorageReader;
    private final LatestVotingProcedureService latestVotingProcedureService;
    private final LatestVotingProcedureMapper latestVotingProcedureMapper;
    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleLatestVotingProcedure(GovernanceEvent governanceEvent) {
        EventMetadata eventMetadata = governanceEvent.getMetadata();

        Map<LatestVotingProcedureId, LatestVotingProcedure> latestVotingProcedureMap = new HashMap<>();
        int index = 0;

        for (TxGovernance txGovernance : governanceEvent.getTxGovernanceList()) {
            if (txGovernance.getVotingProcedures() == null) {
                continue;
            }

            Map<Voter, Map<GovActionId, com.bloxbean.cardano.yaci.core.model.governance.VotingProcedure>>
                    voting = txGovernance.getVotingProcedures().getVoting();

            for (var entry : voting.entrySet()) {
                Voter voter = entry.getKey();
                var votingInfoMap = entry.getValue();

                for (var votingInfoEntry : votingInfoMap.entrySet()) {
                    var govActionId = votingInfoEntry.getKey();
                    var votingInfo = votingInfoEntry.getValue();
                    LatestVotingProcedure latestVotingProcedure = LatestVotingProcedure
                            .builder().id(UUID.randomUUID())
                            .voterType(voter.getType())
                            .voterHash(voter.getHash())
                            .index(index++)
                            .vote(votingInfo.getVote())
                            .txHash(txGovernance.getTxHash())
                            .govActionTxHash(govActionId.getTransactionId())
                            .govActionIndex(govActionId.getGov_action_index())
                            .build();

                    latestVotingProcedure.setSlot(eventMetadata.getSlot());
                    latestVotingProcedure.setBlockNumber(eventMetadata.getBlock());
                    latestVotingProcedure.setBlockTime(eventMetadata.getBlockTime());
                    latestVotingProcedure.setEpoch(eventMetadata.getEpochNumber());

                    if (votingInfo.getAnchor() != null) {
                        latestVotingProcedure.setAnchorUrl(votingInfo.getAnchor().getAnchor_url());
                        latestVotingProcedure.setAnchorHash(votingInfo.getAnchor().getAnchor_data_hash());
                    }

                    var latestVotingProcedureId = LatestVotingProcedureId.builder()
                            .voterHash(voter.getHash())
                            .govActionTxHash(govActionId.getTransactionId())
                            .govActionIndex(govActionId.getGov_action_index())
                            .build();

                    latestVotingProcedureMap.put(latestVotingProcedureId, latestVotingProcedure);
                }
            }
        }

        if (latestVotingProcedureMap.isEmpty()) {
            return;
        }

        latestVotingProcedureStorage.saveOrUpdate(latestVotingProcedureMap.values());

//        List<LatestVotingProcedure> savedVotingProcedure = latestVotingProcedureStorageReader
//                .getAllByIdIn(latestVotingProcedureMap.values().stream()
//                        .map(latestVotingProcedure -> LatestVotingProcedureId.builder()
//                                .voterHash(latestVotingProcedure.getVoterHash())
//                                .govActionTxHash(latestVotingProcedure.getGovActionTxHash())
//                                .govActionIndex(latestVotingProcedure.getGovActionIndex())
//                                .build()).toList());
//
//        publisher.publishEvent(VotingEvent.builder()
//                .metadata(eventMetadata)
//                .txVotes(savedVotingProcedure.stream().map(latestVotingProcedureMapper::toTxVote).toList())
//                .build());
    }
}
