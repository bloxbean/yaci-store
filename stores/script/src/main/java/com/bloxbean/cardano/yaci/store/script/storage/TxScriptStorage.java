package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;

import java.util.List;

public interface TxScriptStorage {
    void saveAll(List<TxScript> txScripts);

    int deleteBySlotGreaterThan(long slot);

    List<TxScript> findByTxHash(String txHash);
}
