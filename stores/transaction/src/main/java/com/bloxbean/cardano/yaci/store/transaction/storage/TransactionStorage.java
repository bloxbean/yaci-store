package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;

import java.util.List;

public interface TransactionStorage {
    void saveAll(List<Txn> txList);
    int deleteBySlotGreaterThan(long slot);
    int deleteBySlotLessThan(long slot);
    
    /**
     * Save transaction CBOR data
     * This method is called from processor layer when CBOR storage is enabled
     */
    void saveCbor(List<TxnCbor> txnCborList);
}
