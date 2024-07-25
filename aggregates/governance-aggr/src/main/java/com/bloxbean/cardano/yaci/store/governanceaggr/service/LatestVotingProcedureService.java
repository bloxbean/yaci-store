package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LatestVotingProcedureService {

    private static final int DEFAULT_PAGE_SIZE = 1000;
    private final VotingProcedureStorageReader votingProcedureStorageReader;
    private final LatestVotingProcedureStorageReader latestVotingProcedureStorageReader;
    private final LatestVotingProcedureStorage latestVotingProcedureStorage;
    private final LatestVotingProcedureMapper latestVotingProcedureMapper;

    public void syncUpLatestVotingProcedure() {
        long startTime = System.currentTimeMillis();
        log.info("Sync up Latest Voting Procedure: -------Start------");

        Long latestSlot = latestVotingProcedureStorageReader.findLatestSlotOfVotingProcedure().orElse(0L);
        Pageable pageable =
                PageRequest.of(
                        0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "slot"));

        Slice<VotingProcedure> votingProcedureSlice =
                votingProcedureStorageReader.findBySlotGreaterThan(latestSlot, pageable);
        saveLatestVotingProcedure(votingProcedureSlice.getContent());

        while (votingProcedureSlice.hasNext()) {
            pageable = votingProcedureSlice.nextPageable();
            votingProcedureSlice =
                    votingProcedureStorageReader.findBySlotGreaterThan(
                            latestSlot, pageable);
            saveLatestVotingProcedure(votingProcedureSlice.getContent());
        }

        log.info(
                "Sync up Latest Voting Procedure: -------End------, time: {} ms",
                System.currentTimeMillis() - startTime);
    }

    void saveLatestVotingProcedure(List<VotingProcedure> votingProcedureList) {
        log.info("Processing {} voting procedures", votingProcedureList.size());
        List<LatestVotingProcedureId> votingProcedureIds =
                votingProcedureList.stream()
                        .map(votingProcedure -> buildVotingProcedureId(votingProcedure.getGovActionTxHash(),
                                votingProcedure.getGovActionIndex(), votingProcedure.getVoterHash()))
                        .collect(Collectors.toList());

        Map<LatestVotingProcedureId, LatestVotingProcedure> latestVotingProcedureMap =
                latestVotingProcedureStorageReader.getAllByIdIn(votingProcedureIds).stream()
                        .collect(
                                Collectors.toMap(latestVotingProcedure -> buildVotingProcedureId(
                                        latestVotingProcedure.getGovActionTxHash(), latestVotingProcedure.getGovActionIndex(),
                                        latestVotingProcedure.getVoterHash()), Function.identity()));

        votingProcedureList.forEach(
                votingProcedure -> {
                    LatestVotingProcedureId latestVotingProcedureId =
                            buildVotingProcedureId(votingProcedure.getGovActionTxHash(),
                                    votingProcedure.getGovActionIndex(), votingProcedure.getVoterHash());
                    LatestVotingProcedure latestVotingProcedure =
                            latestVotingProcedureMap.get(latestVotingProcedureId);
                    if (latestVotingProcedure == null) {
                        latestVotingProcedure = latestVotingProcedureMapper.fromVotingProcedure(votingProcedure);
                        latestVotingProcedure.setRepeatVote(false);
                    } else if (!latestVotingProcedure.getId().equals(votingProcedure.getId())) {
                        latestVotingProcedure.setVoteInPrevAggrSlot(latestVotingProcedure.getVote());
                        latestVotingProcedureMapper.updateByVotingProcedure(latestVotingProcedure, votingProcedure);
                        latestVotingProcedure.setRepeatVote(true);
                    }

                    latestVotingProcedureMap.put(latestVotingProcedureId, latestVotingProcedure);
                });

        log.info("Saving {} latest voting procedures", latestVotingProcedureMap.size());
        latestVotingProcedureStorage.saveAll(latestVotingProcedureMap.values().stream().toList());
    }

    private LatestVotingProcedureId buildVotingProcedureId(String govTxHash, Integer govIdx, String voterHash) {
        return LatestVotingProcedureId.builder()
                .govActionTxHash(govTxHash)
                .govActionIndex(govIdx)
                .voterHash(voterHash)
                .build();
    }
}
