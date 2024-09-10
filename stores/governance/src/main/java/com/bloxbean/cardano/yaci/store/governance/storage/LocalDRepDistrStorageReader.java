package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalDRepDistr;

import java.util.Optional;

public interface LocalDRepDistrStorageReader {
    Optional<LocalDRepDistr> findLocalDRepDistrByDRepHashAndEpoch(String dRepHash, Integer epoch);
}
