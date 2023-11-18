package com.bloxbean.cardano.yaci.store.blocks.storage;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;

import java.util.Optional;

public interface EpochStorage {
    void save(Epoch epoch);
    Optional<Epoch> findByNumber(int number);
}
