package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;

public interface CommitteeStorage {
    void save(Committee committee);

    int deleteBySlotGreaterThan(long slot);
}
