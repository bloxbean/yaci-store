package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.VotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.VotingProcedureRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class VotingProcedureStorageImpl implements VotingProcedureStorage {

    private final VotingProcedureRepository votingProcedureRepository;
    private final VotingProcedureMapper votingProcedureMapper;

    @Override
    public void saveAll(List<VotingProcedure> votingProcedures) {
        votingProcedureRepository.saveAll(votingProcedures.stream()
                .map(votingProcedureMapper::toVotingProcedureEntity).collect(Collectors.toList()));
    }

    @Override
    public List<VotingProcedure> findByVoterTypeAndEpochIsGreaterThanEqual(VoterType voterType, int epoch) {
        return votingProcedureRepository.findByVoterTypeAndEpochIsGreaterThanEqual(voterType, epoch).stream()
                .map(votingProcedureMapper::toVotingProcedure).collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return votingProcedureRepository.deleteBySlotGreaterThan(slot);
    }
}
