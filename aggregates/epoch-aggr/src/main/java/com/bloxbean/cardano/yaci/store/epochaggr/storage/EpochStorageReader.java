package com.bloxbean.cardano.yaci.store.epochaggr.storage;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.EpochsPage;

import java.util.Optional;

public interface EpochStorageReader {
    Optional<Epoch> findRecentEpoch();

    EpochsPage findEpochs(int page, int count);

    Optional<Epoch> findByNumber(int number);
}
