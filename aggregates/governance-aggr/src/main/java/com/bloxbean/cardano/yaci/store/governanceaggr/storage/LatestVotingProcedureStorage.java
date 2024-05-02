package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;

import java.util.Collection;
import java.util.List;

public interface LatestVotingProcedureStorage {
    void saveAll(List<LatestVotingProcedure> latestVotingProcedure);
    void saveOrUpdate(Collection<LatestVotingProcedure> latestVotingProcedures);
    int deleteBySlotGreaterThan(long slot);
}
