package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;

import java.util.List;

public interface TxScriptStorageReader {
    List<TxScript> findByTxHash(String txHash);
}
