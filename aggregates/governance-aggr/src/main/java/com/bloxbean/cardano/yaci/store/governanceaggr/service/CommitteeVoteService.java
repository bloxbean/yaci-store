package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.*;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommitteeVoteService {
    private final LatestVotingProcedureStorageReader latestVotingProcedureStorageReader;
    private final CommitteeVoteStorageReader committeeVoteStorageReader;
    private final CommitteeVoteStorage committeeVoteStorage;

    public void calculateAndSaveCommitteeVoteBySlotGreaterThan(Long slot) {
        List<LatestVotingProcedure> latestVotingProcedures = latestVotingProcedureStorageReader.findBySlotGreaterThan(slot);

        if (latestVotingProcedures.isEmpty()) {
            return;
        }

        calculateAndSaveCommitteeVoteByLatestVotingProcedureList(latestVotingProcedures);
    }

    public void calculateAndSaveCommitteeVoteByLatestVotingProcedureList(List<LatestVotingProcedure> latestVotingProcedures) {

        if (latestVotingProcedures.isEmpty()) {
            return;
        }

        var sortedLatestVotingProcedures = latestVotingProcedures.stream()
                .sorted(Comparator.comparing(LatestVotingProcedure::getSlot)).toList();

        Map<CommitteeVoteId, VoteCount> voteCountMap = calculateVoteCounts(sortedLatestVotingProcedures);

        List<CommitteeVote> committeeVotesToSave = new ArrayList<>();

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
            var committeeVote = committeeVotesMap.get(govActionId);
            if (committeeVote == null) {
                committeeVote = CommitteeVote.builder()
                        .govActionTxHash(committeeVoteId.getGovActionTxHash())
                        .govActionIndex(committeeVoteId.getGovActionIndex())
                        .slot(committeeVoteId.getSlot())
                        .yesCnt(voteCount.getYes())
                        .noCnt(voteCount.getNo())
                        .abstainCnt(voteCount.getAbstain())
                        .build();
            } else {
                committeeVote.setSlot(committeeVoteId.getSlot());
                committeeVote.setYesCnt(committeeVote.getYesCnt() + voteCount.getYes());
                committeeVote.setNoCnt(committeeVote.getNoCnt() + voteCount.getNo());
                committeeVote.setAbstainCnt(committeeVote.getAbstainCnt() + voteCount.getAbstain());
            }
            committeeVotesToSave.add(committeeVote);
        });

        if (!committeeVotesToSave.isEmpty()) {
            committeeVoteStorage.saveAll(committeeVotesToSave);
        }
    }

    public Map<CommitteeVoteId, VoteCount> calculateVoteCounts(List<LatestVotingProcedure> latestVotingProcedures) {
        Map<CommitteeVoteId, VoteCount> voteCountMap = new LinkedHashMap<>();

        for (LatestVotingProcedure voting : latestVotingProcedures) {
            if (!voting.getVoterType().equals(VoterType.CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH)) {
                continue;
            }

            CommitteeVoteId committeeVoteId = CommitteeVoteId.builder()
                    .govActionIndex(voting.getGovActionIndex())
                    .govActionTxHash(voting.getGovActionTxHash())
                    .slot(voting.getSlot())
                    .build();

            VoteCount voteCount = voteCountMap.computeIfAbsent(committeeVoteId, k -> new VoteCount());

            updateVoteCount(voteCount, voting.getVote(), true);
            updateVoteCount(voteCount, voting.getVoteInPrevAggrSlot(), false);
        }

        return voteCountMap;
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

}
