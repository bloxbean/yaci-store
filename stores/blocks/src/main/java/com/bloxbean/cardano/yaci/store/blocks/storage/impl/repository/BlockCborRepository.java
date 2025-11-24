package com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockCborEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockCborRepository extends JpaRepository<BlockCborEntity, String> {
    
    /**
     * Find block CBOR data by block hash
     */
    Optional<BlockCborEntity> findByBlockHash(String blockHash);

    /**
     * Delete block CBOR data by slot greater than specified value
     * Used for rollback handling
     */
    int deleteBySlotGreaterThan(long slot);

    /**
     * Delete block CBOR data by slot less than specified value
     * Used for pruning old data
     */
    int deleteBySlotLessThan(long slot);
}


