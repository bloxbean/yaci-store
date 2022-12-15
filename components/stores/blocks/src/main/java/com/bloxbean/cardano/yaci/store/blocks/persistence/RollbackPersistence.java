package com.bloxbean.cardano.yaci.store.blocks.persistence;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;

public interface RollbackPersistence {
    void save(RollbackEvent rollbackEvent);
}
