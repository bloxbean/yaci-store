package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;

public interface LocalConstitutionStorage {
    void save(LocalConstitution localConstitution);

    int deleteBySlotGreaterThan(long slot);
}
