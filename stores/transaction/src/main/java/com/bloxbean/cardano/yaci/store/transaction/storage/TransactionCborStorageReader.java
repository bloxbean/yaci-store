package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;

import java.util.Optional;

/**
 * Reader interface for transaction CBOR data
 */
public interface TransactionCborStorageReader {
    
    /**
     * Get transaction CBOR data by transaction hash
     * 
     * @param txHash Transaction hash
     * @return TxnCbor domain object, or empty if not found
     */
    Optional<TxnCbor> getTxCborByHash(String txHash);
    
    /**
     * Check if CBOR data exists for a transaction
     * 
     * @param txHash Transaction hash
     * @return true if CBOR data exists
     */
    boolean cborExists(String txHash);
}

