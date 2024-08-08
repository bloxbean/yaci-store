package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommittee;

public interface LocalCommitteeStorage {
    void save(LocalCommittee localCommittee);

    int deleteBySlotGreaterThan(long slot);
}
