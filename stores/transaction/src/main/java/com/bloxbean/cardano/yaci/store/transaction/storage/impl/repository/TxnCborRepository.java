package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnCborEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TxnCborRepository extends JpaRepository<TxnCborEntity, String> {
    
    /**
     * Find transaction CBOR data by transaction hash
     */
    Optional<TxnCborEntity> findByTxHash(String txHash);

    /**
     * Delete transaction CBOR data by slot greater than specified value
     * Used for rollback handling
     */
    int deleteBySlotGreaterThan(long slot);

    /**
     * Delete transaction CBOR data by slot less than specified value
     * Used for pruning old data
     */
    int deleteBySlotLessThan(long slot);
}


