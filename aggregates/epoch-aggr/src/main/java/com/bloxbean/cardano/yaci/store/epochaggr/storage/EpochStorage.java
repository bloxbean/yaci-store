package com.bloxbean.cardano.yaci.store.epochaggr.storage;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;

import java.util.Optional;

public interface EpochStorage {
    void save(Epoch epoch);
    Optional<Epoch> findByNumber(int number);
}
