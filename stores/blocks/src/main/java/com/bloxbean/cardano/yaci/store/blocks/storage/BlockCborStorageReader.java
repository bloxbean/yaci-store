package com.bloxbean.cardano.yaci.store.blocks.storage;

import java.util.Optional;

/**
 * Reader interface for block CBOR data
 */
public interface BlockCborStorageReader {
    
    /**
     * Get block CBOR data by block hash
     * 
     * @param blockHash Block hash
     * @return CBOR data bytes, or empty if not found or CBOR storage is disabled
     */
    Optional<byte[]> getBlockCborByHash(String blockHash);
    
    /**
     * Check if CBOR data exists for a block
     * 
     * @param blockHash Block hash
     * @return true if CBOR data exists
     */
    boolean cborExists(String blockHash);
}


