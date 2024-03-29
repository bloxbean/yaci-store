package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;

import java.util.List;

public interface VotingProcedureStorage {
    void saveAll(List<VotingProcedure> votingProcedures);
    int deleteBySlotGreaterThan(long slot);
}
