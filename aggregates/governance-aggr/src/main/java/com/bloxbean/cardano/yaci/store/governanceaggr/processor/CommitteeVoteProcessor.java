package com.bloxbean.cardano.yaci.store.governanceaggr.processor;


import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.event.VotingEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorageReader;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.model.governance.VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeVoteProcessor {
    private final CommitteeVoteStorageReader committeeVoteStorageReader;
    private final CommitteeVoteStorage committeeVoteStorage;
    private final Map<Long, VotingEvent> votingEventsMap = new ConcurrentHashMap<>();

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
            Map<CommitteeVoteId, VoteCount> voteCountMap = new LinkedHashMap<>();
            List<VotingEvent> sortedVotingEvents = votingEventsMap.entrySet().stream()
                    .sorted(Comparator.comparingLong(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            for (VotingEvent votingEvent : sortedVotingEvents) {
                votingEvent.getTxVotes().stream()
                        .filter(txVote -> txVote.getVoterType().equals(CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH))
                        .forEach(txVote -> {
                            CommitteeVoteId committeeVoteId = CommitteeVoteId.builder()
                                    .govActionIndex(txVote.getGovActionIndex())
                                    .govActionTxHash(txVote.getGovActionTxHash())
                                    .slot(votingEvent.getMetadata().getSlot())
                                    .build();

                            VoteCount voteCount = voteCountMap.computeIfAbsent(committeeVoteId, k -> new VoteCount());

                            updateVoteCount(voteCount, txVote.getVote(), true);
                            updateVoteCount(voteCount, txVote.getVoteInPrevAggrSlot(), false);
                        });
            }
            List<CommitteeVote> committeeVoteToSave = new ArrayList<>();

            Map<GovActionId, CommitteeVote> committeeVotesMap =
                    committeeVoteStorageReader.findByGovActionTxHashAndGovActionIndexPairsWithMaxSlot(
                                    voteCountMap.keySet().stream().map(committeeVoteId ->
                                            GovActionId.builder()
                                                    .govActionTxHash(committeeVoteId.getGovActionTxHash())
                                                    .govActionIndex(committeeVoteId.getGovActionIndex())
                                                    .build()).toList())
                            .stream()
                            .collect(Collectors.toMap(committeeVote -> GovActionId.builder()
                                    .govActionTxHash(committeeVote.getGovActionTxHash())
                                    .govActionIndex(committeeVote.getGovActionIndex())
                                    .build(), Function.identity()));

            voteCountMap.forEach((committeeVoteId, voteCount) -> {
                var govActionId = GovActionId.builder()
                        .govActionTxHash(committeeVoteId.getGovActionTxHash())
                        .govActionIndex(committeeVoteId.getGovActionIndex())
                        .build();
                var committeeVotes = committeeVotesMap.get(govActionId);
                if (committeeVotes == null) {
                    committeeVotes = CommitteeVote.builder()
                            .govActionTxHash(committeeVoteId.getGovActionTxHash())
                            .govActionIndex(committeeVoteId.getGovActionIndex())
                            .slot(committeeVoteId.getSlot())
                            .yesCnt(voteCount.getYes())
                            .noCnt(voteCount.getNo())
                            .abstainCnt(voteCount.getAbstain())
                            .build();
                } else {
                    committeeVotes.setSlot(committeeVoteId.getSlot());
                    committeeVotes.setYesCnt(committeeVotes.getYesCnt() + voteCount.getYes());
                    committeeVotes.setNoCnt(committeeVotes.getNoCnt() + voteCount.getNo());
                    committeeVotes.setAbstainCnt(committeeVotes.getAbstainCnt() + voteCount.getAbstain());
                }
                committeeVoteToSave.add(committeeVotes);
            });
            committeeVoteStorage.saveAll(committeeVoteToSave);
        } finally {
            votingEventsMap.clear();
        }
    }

    private void updateVoteCount(VoteCount voteCount, Vote vote, boolean isAdd) {
        if (vote == null) {
            return;
        }

        switch (vote) {
            case YES:
                if (isAdd) voteCount.addYes();
                else voteCount.subtractYes();
                break;
            case NO:
                if (isAdd) voteCount.addNo();
                else voteCount.subtractNo();
                break;
            default:
                if (isAdd) voteCount.addAbstain();
                else voteCount.subtractAbstain();
                break;
        }
    }

    @Getter
    @Setter
    protected static class VoteCount {
        int yes;
        int no;
        int abstain;

        public VoteCount() {
            this.yes = 0;
            this.no = 0;
            this.abstain = 0;
        }

        void addYes() {
            this.yes++;
        }

        void addNo() {
            this.no++;
        }

        void addAbstain() {
            this.abstain++;
        }

        void subtractYes() {
            this.yes--;
        }

        void subtractNo() {
            this.no--;
        }

        void subtractAbstain() {
            this.abstain--;
        }
    }

    @EqualsAndHashCode
    @Builder
    @Getter
    @Setter
    private static class CommitteeVoteId {
        private String govActionTxHash;
        private int govActionIndex;
        private long slot;
    }
}
