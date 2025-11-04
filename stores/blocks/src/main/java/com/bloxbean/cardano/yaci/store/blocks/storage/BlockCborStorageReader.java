package com.bloxbean.cardano.yaci.store.blocks.storage;

import com.bloxbean.cardano.yaci.store.blocks.domain.BlockCbor;

import java.util.Optional;

/**
 * Reader interface for block CBOR data
 */
public interface BlockCborStorageReader {
    
    /**
     * Get block CBOR data by block hash
     * 
     * @param blockHash Block hash
     * @return BlockCbor domain object, or empty if not found
     */
    Optional<BlockCbor> getBlockCborByHash(String blockHash);
    
    /**
     * Check if CBOR data exists for a block
     * 
     * @param blockHash Block hash
     * @return true if CBOR data exists
     */
    boolean cborExists(String blockHash);
}


