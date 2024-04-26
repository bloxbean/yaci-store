package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.LatestVotingProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LatestVotingProcedureStorageImpl implements LatestVotingProcedureStorage {
    private final LatestVotingProcedureRepository latestVotingProcedureRepository;
    private final LatestVotingProcedureMapper latestVotingProcedureMapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<LatestVotingProcedure> latestVotingProcedure) {
        latestVotingProcedureRepository.saveAll(latestVotingProcedure.stream()
                .map(latestVotingProcedureMapper::toLatestVotingProcedureEntity).collect(Collectors.toList()));
    }
}
