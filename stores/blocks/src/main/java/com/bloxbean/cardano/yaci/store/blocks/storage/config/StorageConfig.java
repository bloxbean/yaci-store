package com.bloxbean.cardano.yaci.store.blocks.storage.config;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;

public interface StorageConfig {

    BlockStorage blockStorage();

    BlockStorageReader blockStorageReader();

    RollbackStorage rollbackStorage();
}
