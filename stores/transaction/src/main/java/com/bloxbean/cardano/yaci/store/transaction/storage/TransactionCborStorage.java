package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.util.List;

/**
 * Storage interface for transaction CBOR data
 */
public interface TransactionCborStorage {
    
    /**
     * Save CBOR data for multiple transactions
     * 
     * @param txnList List of transactions with CBOR data
     */
    void saveAll(List<Txn> txnList);
    
    /**
     * Delete CBOR data by slot greater than specified value
     * Used for rollback handling
     * 
     * @param slot Slot threshold
     * @return Number of records deleted
     */
    int deleteBySlotGreaterThan(long slot);
    
    /**
     * Delete CBOR data by slot less than specified value
     * Used for pruning old data
     * 
     * @param slot Slot threshold
     * @return Number of records deleted
     */
    int deleteBySlotLessThan(long slot);
}

