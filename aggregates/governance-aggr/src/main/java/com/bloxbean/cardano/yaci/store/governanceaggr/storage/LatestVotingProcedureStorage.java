package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LatestVotingProcedureStorage {
    void saveAll(List<LatestVotingProcedure> latestVotingProcedure);
    void saveOrUpdate(Collection<LatestVotingProcedure> latestVotingProcedures);
    Optional<Long> findLatestSlotOfVotingProcedure();
    List<LatestVotingProcedure> getAllByIdIn(List<LatestVotingProcedureId> votingProcedureIds);
    List<LatestVotingProcedure> findByVoterTypeAndEpochIsGreaterThanEqual(VoterType voterType, int epoch);
    int deleteBySlotGreaterThan(long slot);
}
