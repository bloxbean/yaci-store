package com.bloxbean.cardano.yaci.store.governanceaggr.processor;


import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVoteId;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.VoteCount;
import com.bloxbean.cardano.yaci.store.governanceaggr.event.VotingEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.CommitteeVoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.model.governance.VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeVoteProcessor {
    private final Map<Long, VotingEvent> votingEventsMap = new ConcurrentHashMap<>();
    private final CommitteeVoteService committeeVoteService;

    @EventListener
    @Transactional
    public void handleCommitteeVotingProcedure(VotingEvent votingEvent) {
        votingEventsMap.put(votingEvent.getMetadata().getSlot(), votingEvent);
    }

    @EventListener
    @Transactional
    public void handleCommitEvent(CommitEvent commitEvent) {
        if (votingEventsMap.isEmpty()) {
            return;
        }

        try {
            List<VotingEvent> sortedVotingEvents = votingEventsMap.entrySet().stream()
                    .sorted(Comparator.comparingLong(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            List<LatestVotingProcedure> latestVotingProcedures = new ArrayList<>();

            for (VotingEvent votingEvent : sortedVotingEvents) {
                latestVotingProcedures.addAll(votingEvent.getTxVotes().stream()
                        .filter(txVote -> txVote.getVoterType().equals(CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH))
                        .map(txVote ->
                                LatestVotingProcedure.builder()
                                        .slot(votingEvent.getMetadata().getSlot())
                                        .govActionTxHash(txVote.getGovActionTxHash())
                                        .govActionIndex(txVote.getGovActionIndex())
                                        .voterType(txVote.getVoterType())
                                        .voterHash(txVote.getVoterHash())
                                        .vote(txVote.getVote())
                                        .voteInPrevAggrSlot(txVote.getVoteInPrevAggrSlot())
                                        .build()).collect(Collectors.toList()));
            }
            committeeVoteService.calculateAndSaveCommitteeVoteByLatestVotingProcedureList(latestVotingProcedures);
        } finally {
            votingEventsMap.clear();
        }
    }

}
