package com.bloxbean.cardano.yaci.store.transaction.storage;

import java.util.Optional;

/**
 * Reader interface for transaction CBOR data
 */
public interface TransactionCborStorageReader {
    
    /**
     * Get transaction CBOR data by transaction hash
     * 
     * @param txHash Transaction hash
     * @return CBOR data bytes, or empty if not found or CBOR storage is disabled
     */
    Optional<byte[]> getTxCborByHash(String txHash);
    
    /**
     * Check if CBOR data exists for a transaction
     * 
     * @param txHash Transaction hash
     * @return true if CBOR data exists
     */
    boolean cborExists(String txHash);
}

