package com.bloxbean.cardano.yaci.store.epoch.storage.api;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;

import java.util.Optional;

public interface EpochParamStorage {
    void save(EpochParam epochParam);
    Optional<EpochParam> getProtocolParams(int epoch);

    Integer getMaxEpoch();
    int deleteBySlotGreaterThan(long slot);
}
