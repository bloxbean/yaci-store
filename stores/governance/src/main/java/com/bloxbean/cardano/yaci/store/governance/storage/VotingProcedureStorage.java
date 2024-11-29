package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;

import java.util.List;

public interface VotingProcedureStorage {
    void saveAll(List<VotingProcedure> votingProcedures);
    List<VotingProcedure> findByVoterTypeAndEpochIsGreaterThanEqual(VoterType voterType, int epoch);
    int deleteBySlotGreaterThan(long slot);
}
