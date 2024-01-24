package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VotingProcedureProcessor {

    private final VotingProcedureStorage votingProcedureStorage;

    @EventListener
    @Transactional
    public void handleVotingProcedure(GovernanceEvent governanceEvent) {
        EventMetadata eventMetadata = governanceEvent.getMetadata();

        List<VotingProcedure> votingProcedures = new ArrayList<>();
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
                    VotingProcedure votingProcedure = new VotingProcedure();

                    var govActionId = votingInfoEntry.getKey();
                    var votingInfo = votingInfoEntry.getValue();

                    votingProcedure.setVoterType(voter.getType());
                    votingProcedure.setVoterHash(voter.getHash());
                    votingProcedure.setIndex(index++);
                    votingProcedure.setVote(votingInfo.getVote());
                    votingProcedure.setTxHash(txGovernance.getTxHash());
                    votingProcedure.setGovActionTxHash(govActionId.getTransactionId());
                    votingProcedure.setGovActionIndex(govActionId.getGov_action_index());

                    if (votingInfo.getAnchor() != null) {
                        votingProcedure.setAnchorUrl(votingInfo.getAnchor().getAnchor_url());
                        votingProcedure.setAnchorHash(votingInfo.getAnchor().getAnchor_data_hash());
                    }

                    votingProcedure.setSlot(eventMetadata.getSlot());
                    votingProcedure.setBlockNumber(eventMetadata.getBlock());
                    votingProcedure.setBlockTime(eventMetadata.getBlockTime());
                    votingProcedure.setEpoch(eventMetadata.getEpochNumber());

                    votingProcedures.add(votingProcedure);
                }
            }
        }

        if (!votingProcedures.isEmpty()) {
            votingProcedureStorage.saveAll(votingProcedures);
        }
    }
}
