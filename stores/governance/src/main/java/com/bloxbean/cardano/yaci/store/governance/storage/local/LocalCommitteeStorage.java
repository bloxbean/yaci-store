package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;

public interface LocalCommitteeStorage {
    void save(LocalCommittee localCommittee);

    int deleteBySlotGreaterThan(long slot);
}
