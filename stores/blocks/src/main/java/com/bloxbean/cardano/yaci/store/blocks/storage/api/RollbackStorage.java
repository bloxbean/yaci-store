package com.bloxbean.cardano.yaci.store.blocks.storage.api;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;

public interface RollbackStorage {
    void save(RollbackEvent rollbackEvent);
}
