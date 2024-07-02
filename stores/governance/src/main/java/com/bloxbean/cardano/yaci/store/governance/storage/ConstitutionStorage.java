package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;

public interface ConstitutionStorage {
    void save(Constitution constitution);
    int deleteBySlotGreaterThan(long slot);
}
