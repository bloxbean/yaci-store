package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.VotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.VotingProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class VotingProcedureStorageReaderImpl implements VotingProcedureStorageReader {
    private final VotingProcedureRepository votingProcedureRepository;
    private final VotingProcedureMapper votingProcedureMapper;

    @Override
    public List<VotingProcedure> findByTxHash(String txHash) {
        List<VotingProcedureEntity> votingProcedureEntities = votingProcedureRepository.findByTxHash(txHash);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

    @Override
    public List<VotingProcedure> findByGovActionTxHash(String govActionTxHash, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        Slice<VotingProcedureEntity> votingProcedureEntities = votingProcedureRepository.findByGovActionTxHash(govActionTxHash, sortedBySlot);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

    @Override
    public List<VotingProcedure> findByGovActionTxHashAndGovActionIndex(String govActionTxHash, int govActionIndex) {
        List<VotingProcedureEntity> votingProcedureEntities = votingProcedureRepository.findByGovActionTxHashAndIndex(govActionTxHash, govActionIndex);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

}
