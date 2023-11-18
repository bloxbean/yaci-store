package com.bloxbean.cardano.yaci.store.blocks.storage;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;

import java.util.Optional;

public interface EpochStorageReader {
    Optional<Epoch> findRecentEpoch();

    EpochsPage findEpochs(int page, int count);

    Optional<Epoch> findByNumber(int number);
}
