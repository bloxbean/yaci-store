package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;

import java.util.Optional;

public interface AdaPotStorage {
    void save(AdaPot adaPot);

    Optional<AdaPot> findByEpoch(int epoch);
}
