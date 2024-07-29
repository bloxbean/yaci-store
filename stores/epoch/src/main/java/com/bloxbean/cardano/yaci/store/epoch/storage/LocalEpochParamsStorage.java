package com.bloxbean.cardano.yaci.store.epoch.storage;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;

import java.util.Optional;

public interface LocalEpochParamsStorage {
    void save(EpochParam epochParam);

    Optional<EpochParam> getEpochParam(int epoch);

    Optional<EpochParam> getLatestEpochParam();

    Optional<Integer> getMaxEpoch();
}
