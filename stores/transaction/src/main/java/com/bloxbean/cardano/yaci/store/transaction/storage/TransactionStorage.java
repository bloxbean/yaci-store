package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.util.List;

public interface TransactionStorage {
    void saveAll(List<Txn> txList);
    int deleteBySlotGreaterThan(long slot);
    int deleteBySlotLessThan(long slot);
    
    /**
     * Save transaction CBOR data
     * This method is called from processor layer when CBOR storage is enabled
     */
    void saveCbor(List<Txn> txnList);
}
