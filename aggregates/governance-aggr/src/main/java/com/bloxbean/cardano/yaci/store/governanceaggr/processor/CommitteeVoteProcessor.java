package com.bloxbean.cardano.yaci.store.governanceaggr.processor;


import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.core.model.governance.VotingProcedure;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeVoteProcessor {
    private final Map<CommitteeVoteId, VoteCount> voteCountMap = new ConcurrentHashMap<>();
    private final CommitteeVoteStorageReader committeeVoteStorageReader;

    @EventListener
    @Transactional
    public void handleCommitteeVotingProcedure(GovernanceEvent governanceEvent) {

        for (TxGovernance txGovernance : governanceEvent.getTxGovernanceList()) {
            if (txGovernance.getVotingProcedures() == null) {
                continue;
            }

            Map<Voter, Map<com.bloxbean.cardano.yaci.core.model.governance.GovActionId, VotingProcedure>>
                    voting = txGovernance.getVotingProcedures().getVoting();

            for (var entry : voting.entrySet()) {
                Voter voter = entry.getKey();
                var votingInfoMap = entry.getValue();
                if (voter.getType().equals(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH)) {
                    for (var votingInfoEntry : votingInfoMap.entrySet()) {
                        var govActionId = votingInfoEntry.getKey();
                        var votingInfo = votingInfoEntry.getValue();
                        var voteCount = voteCountMap.putIfAbsent(CommitteeVoteId.builder()
                                .govActionIndex(govActionId.getGov_action_index())
                                .govActionTxHash(govActionId.getTransactionId())
                                .slot(governanceEvent.getMetadata().getSlot())
                                .build(), new VoteCount());

                        if (votingInfo.getVote().equals(Vote.YES)) {
                            voteCount.plusYes();
                        } else if (votingInfo.getVote().equals(Vote.NO)) {
                            voteCount.plusNo();
                        } else {
                            voteCount.plusAbstain();
                        }
                    }
                }
            }
        }
    }

    @EventListener
    @Transactional
    public void handleCommitEvent(CommitEvent commitEvent) {
        try {
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

        } finally {
            voteCountMap.clear();
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

        void plusYes() {
            this.yes++;
        }

        void plusNo() {
            this.no++;
        }

        void plusAbstain() {
            this.abstain++;
        }
    }
}
