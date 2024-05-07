package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.LatestVotingProcedureRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class LatestVotingProcedureStorageReaderImpl implements LatestVotingProcedureStorageReader {
    private final LatestVotingProcedureRepository latestVotingProcedureRepository;
    private final LatestVotingProcedureMapper latestVotingProcedureMapper;

    @Override
    public Optional<Long> findLatestSlotOfVotingProcedure() {
        return latestVotingProcedureRepository.findLatestSlotOfVotingProcedure();
    }

    @Override
    public List<LatestVotingProcedure> getAllByIdIn(List<LatestVotingProcedureId> votingProcedureIds) {
        return latestVotingProcedureRepository.getAllByIdIn(votingProcedureIds).stream()
                .map(latestVotingProcedureMapper::toLatestVotingProcedure)
                .toList();
    }

    @Override
    public List<LatestVotingProcedure> findBySlotGreaterThan(Long slot) {
        return latestVotingProcedureRepository.findBySlotGreaterThan(slot)
                .stream()
                .map(latestVotingProcedureMapper::toLatestVotingProcedure)
                .toList();
    }
}
