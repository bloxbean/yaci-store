package com.bloxbean.cardano.yaci.store.blocks.persistence;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;

import java.util.Optional;

public interface EpochPersistence {
    Optional<Epoch> findRecentEpoch();

    void save(Epoch epoch);

    Optional<Epoch> findByNumber(int number);
    EpochsPage findEpochs(int page, int count);
}
