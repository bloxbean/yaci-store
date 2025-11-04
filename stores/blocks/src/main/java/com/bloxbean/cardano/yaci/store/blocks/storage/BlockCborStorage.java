package com.bloxbean.cardano.yaci.store.blocks.storage;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;

/**
 * Storage interface for block CBOR data
 */
public interface BlockCborStorage {
    
    /**
     * Save CBOR data for a block
     * 
     * @param block Block with CBOR data
     */
    void save(Block block);
    
    /**
     * Delete CBOR data by slot greater than specified value
     * Used for rollback handling
     * 
     * @param slot Slot threshold
     * @return Number of records deleted
     */
    int deleteBySlotGreaterThan(long slot);
}

