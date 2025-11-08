package com.bloxbean.cardano.yaci.store.blocks.storage;

import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;

public interface BlockCborStorage {
    void save(BlockCbor blockCbor);

    int deleteBySlotGreaterThan(long slot);

    int deleteBySlotLessThan(long slot);
}
