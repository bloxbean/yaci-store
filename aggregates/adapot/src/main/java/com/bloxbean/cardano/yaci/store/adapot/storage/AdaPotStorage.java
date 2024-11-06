package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;

import java.util.Optional;

public interface AdaPotStorage {
    void save(AdaPot adaPot);

    /**
     * Adapot at epoch boundary of specified epoch
     * @param epoch
     * @return
     */
    Optional<AdaPot> findByEpoch(long epoch);

    int deleteBySlotGreaterThan(long slot);
}
