package com.bloxbean.cardano.yaci.store.governanceaggr.processor;


import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.Voter;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GovernanceEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxGovernance;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeVoteProcessor {
    private final Map<Pair<Long, GovActionId>, VoteInfo> voteInfoMap = new ConcurrentHashMap<>();
    private final CommitteeVoteStorage committeeVoteStorage;

    @EventListener
    @Transactional
    public void handleVotingProcedure(GovernanceEvent governanceEvent) {
        if (governanceEvent.getTxGovernanceList() == null || governanceEvent.getTxGovernanceList().isEmpty())
            return;

        EventMetadata eventMetadata = governanceEvent.getMetadata();

        for (TxGovernance txGovernance : governanceEvent.getTxGovernanceList()) {
            if (txGovernance.getVotingProcedures() == null) {
                continue;
            }

            Map<Voter, Map<com.bloxbean.cardano.yaci.core.model.governance.GovActionId,
                    com.bloxbean.cardano.yaci.core.model.governance.VotingProcedure>>
                    voting = txGovernance.getVotingProcedures().getVoting();

            for (var entry : voting.entrySet()) {
                Voter voter = entry.getKey();
                var votingInfoMap = entry.getValue();
                if (voter.getType().equals(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH)) {
                    for (var votingInfoEntry : votingInfoMap.entrySet()) {
                        var govActionId = votingInfoEntry.getKey();
                        var votingInfo = votingInfoEntry.getValue();

                        VoteInfo voteInfo = new VoteInfo();
                        voteInfo.setVoterHash(voter.getHash());
                        voteInfo.setVote(votingInfo.getVote());

                        voteInfoMap.put(Pair.of(eventMetadata.getSlot(),
                                        GovActionId.builder()
                                                .govActionTxHash(govActionId.getTransactionId())
                                                .govActionIndex(govActionId.getGov_action_index()).build()),
                                voteInfo);
                    }
                }
            }
        }
    }

    @EventListener
    @Transactional
    public void handleCommitEvent(CommitEvent commitEvent) {
        if (voteInfoMap.isEmpty()) {
            return;
        }

        try {
            List<CommitteeVote> committeeVotesToSave = new ArrayList<>();

            Set<GovActionId> govActionIdSet = voteInfoMap.keySet().stream()
                    .map(Pair::getSecond)
                    .collect(Collectors.toSet());

            Set<Pair<GovActionId, String>> govActionIdVoterHashSet = voteInfoMap.entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey().getSecond(), entry.getValue().getVoterHash()))
                    .collect(Collectors.toSet());

            Map<GovActionId, CommitteeVote> lastCommitteeVoteOfGovActionMap =
                    committeeVoteStorage.findByGovActionIdsWithMaxSlot(govActionIdSet)
                            .stream()
                            .collect(Collectors.toMap(committeeVote ->
                                            GovActionId.builder()
                                                    .govActionTxHash(committeeVote.getGovActionTxHash())
                                                    .govActionIndex(committeeVote.getGovActionIndex())
                                                    .build(),
                                    Function.identity()));

            Map<Pair<GovActionId, String>, CommitteeVote> prevCommitteeVoteOfVoterMap =
                    committeeVoteStorage.findByGovActionIdAndVoterHashWithMaxSlot(govActionIdVoterHashSet)
                            .stream()
                            .collect(Collectors.toMap(committeeVote ->
                                            Pair.of(GovActionId.builder()
                                                            .govActionTxHash(committeeVote.getGovActionTxHash())
                                                            .govActionIndex(committeeVote.getGovActionIndex())
                                                            .build(),
                                                    committeeVote.getVoterHash()),
                                    Function.identity()));

            Map<Pair<Long, GovActionId>, VoteInfo> sortedVoteInfoMap = voteInfoMap.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getFirst()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue,
                            LinkedHashMap::new
                    ));

            sortedVoteInfoMap.forEach((pair, voteInfo) -> {
                GovActionId govActionId = pair.getSecond();
                String voterHash = voteInfo.getVoterHash();
                var lastCommitteeVoteOfGovAction = lastCommitteeVoteOfGovActionMap.get(govActionId);
                var prevCommitteeVoteOfVoter = prevCommitteeVoteOfVoterMap.get(Pair.of(govActionId, voterHash));

                int yesCnt;
                int noCnt;
                int abstainCnt;

                if (lastCommitteeVoteOfGovAction == null) {
                    yesCnt = voteInfo.getVote() == Vote.YES ? 1 : 0;
                    noCnt = voteInfo.getVote() == Vote.NO ? 1 : 0;
                    abstainCnt = voteInfo.getVote() == Vote.ABSTAIN ? 1 : 0;
                } else {
                    if (prevCommitteeVoteOfVoter == null) {
                        yesCnt = (voteInfo.getVote() == Vote.YES ? 1 : 0) + lastCommitteeVoteOfGovAction.getYesCnt();
                        noCnt = (voteInfo.getVote() == Vote.NO ? 1 : 0) + lastCommitteeVoteOfGovAction.getNoCnt();
                        abstainCnt = (voteInfo.getVote() == Vote.ABSTAIN ? 1 : 0) + lastCommitteeVoteOfGovAction.getAbstainCnt();
                    } else {
                        Vote prevVoteOfVoter = prevCommitteeVoteOfVoter.getVote();
                        Vote currentVoteOfVoter = voteInfo.getVote();

                        VoteChange voteChange = getVoteChange(currentVoteOfVoter, prevVoteOfVoter);

                        yesCnt = voteChange.getYes() + lastCommitteeVoteOfGovAction.getYesCnt();
                        noCnt = voteChange.getNo() + lastCommitteeVoteOfGovAction.getNoCnt();
                        abstainCnt = voteChange.getAbstain() + lastCommitteeVoteOfGovAction.getAbstainCnt();
                    }
                }

                CommitteeVote committeeVoteToSave = CommitteeVote.builder()
                        .vote(voteInfo.getVote())
                        .govActionTxHash(govActionId.getGovActionTxHash())
                        .govActionIndex(govActionId.getGovActionIndex())
                        .voterHash(voteInfo.getVoterHash())
                        .slot(pair.getFirst())
                        .yesCnt(yesCnt)
                        .noCnt(noCnt)
                        .abstainCnt(abstainCnt)
                        .build();

                lastCommitteeVoteOfGovActionMap.put(govActionId, committeeVoteToSave);
                prevCommitteeVoteOfVoterMap.put(Pair.of(govActionId, voterHash), committeeVoteToSave);

                committeeVotesToSave.add(committeeVoteToSave);
            });

            if (!committeeVotesToSave.isEmpty()) {
                committeeVoteStorage.saveAll(committeeVotesToSave);
            }
        } finally {
            voteInfoMap.clear();
        }
    }

    private VoteChange getVoteChange(Vote currentVote, Vote prevVote) {
        if (prevVote == null || currentVote.equals(prevVote)) {
            return new VoteChange();
        } else {
            VoteChange voteChange = new VoteChange();
            if (currentVote.equals(Vote.YES)) {
                voteChange.addYes();
            } else if (currentVote.equals(Vote.NO)) {
                voteChange.addNo();
            } else {
                voteChange.addAbstain();
            }

            if (prevVote.equals(Vote.YES)) {
                voteChange.subtractYes();
            } else if (prevVote.equals(Vote.NO)) {
                voteChange.subtractNo();
            } else {
                voteChange.subtractAbstain();
            }

            return voteChange;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class VoteInfo {
        String voterHash;
        Vote vote;
    }

    @Getter
    static class VoteChange {
        int yes = 0;
        int no = 0;
        int abstain = 0;

        public void addYes() {
            this.yes++;
        }

        public void addNo() {
            this.no++;
        }

        public void addAbstain() {
            this.abstain++;
        }

        public void subtractYes() {
            this.yes--;
        }

        public void subtractNo() {
            this.no--;
        }

        public void subtractAbstain() {
            this.abstain--;
        }
    }
}
