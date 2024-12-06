package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;

import java.util.Optional;

public interface CommitteeStorage {
    void save(Committee committee);
    Optional<Committee> getCommitteeByEpoch(int epoch);
    int deleteBySlotGreaterThan(long slot);
}
