package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.VotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.VotingProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class VotingProcedureStorageReaderImpl implements VotingProcedureStorageReader {
    private final VotingProcedureRepository votingProcedureRepository;
    private final VotingProcedureMapper votingProcedureMapper;

    @Override
    public List<VotingProcedure> findAll(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<VotingProcedureEntityJpa> votingProcedureEntities = votingProcedureRepository.findAll(sortedBySlot);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

    @Override
    public Optional<VotingProcedure> findById(UUID id) {
        return votingProcedureRepository.findById(id).map(votingProcedureMapper::toVotingProcedure);
    }

    @Override
    public List<VotingProcedure> findByTxHash(String txHash) {
        List<VotingProcedureEntityJpa> votingProcedureEntities = votingProcedureRepository.findByTxHash(txHash);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

    @Override
    public List<VotingProcedure> findByGovActionTxHash(String govActionTxHash, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<VotingProcedureEntityJpa> votingProcedureEntities = votingProcedureRepository.findByGovActionTxHash(govActionTxHash, sortedBySlot);
        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

    @Override
    public List<VotingProcedure> findByGovActionTxHashAndGovActionIndex(String govActionTxHash, int govActionIndex, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<VotingProcedureEntityJpa> votingProcedureEntities =
                votingProcedureRepository.findByGovActionTxHashAndIndex(govActionTxHash, govActionIndex, sortedBySlot);

        return votingProcedureEntities.stream().map(votingProcedureMapper::toVotingProcedure).toList();
    }

}
