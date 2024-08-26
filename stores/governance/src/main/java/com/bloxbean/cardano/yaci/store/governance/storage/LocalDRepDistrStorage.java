package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalDRepDistr;

import java.util.List;

public interface LocalDRepDistrStorage {
    void saveAll(List<LocalDRepDistr> localDRepDistrList);

    int deleteBySlotGreaterThan(long slot);
}
