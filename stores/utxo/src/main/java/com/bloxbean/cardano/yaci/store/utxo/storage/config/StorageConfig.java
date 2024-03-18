package com.bloxbean.cardano.yaci.store.utxo.storage.config;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;

public interface StorageConfig {

    UtxoStorage utxoStorage();

    UtxoStorageReader utxoStorageReader();
}
