package com.bloxbean.cardano.yaci.store.core.storage.config;

import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;

public interface StorageConfig {

    CursorStorage cursorStorage();

    EraStorage eraStorage();
}
