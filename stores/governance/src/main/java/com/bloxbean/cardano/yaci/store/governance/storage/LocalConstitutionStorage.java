package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalConstitution;

public interface LocalConstitutionStorage {
    void save(LocalConstitution localConstitution);

    int deleteBySlotGreaterThan(long slot);
}
