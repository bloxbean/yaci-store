package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalHardForkInitiation;

import java.util.List;

public interface LocalHardForkInitiationStorage {
    void saveAll(List<LocalHardForkInitiation> localHardForkInitiationList);

    int deleteBySlotGreaterThan(long slot);
}
