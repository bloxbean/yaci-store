package com.bloxbean.cardano.yaci.store.blocks.storage.api;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;

import java.util.Optional;

public interface EpochStorage {
    Optional<Epoch> findRecentEpoch();

    void save(Epoch epoch);

    Optional<Epoch> findByNumber(int number);
    EpochsPage findEpochs(int page, int count);
}
