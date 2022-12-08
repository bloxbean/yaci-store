package com.bloxbean.cardano.yaci.indexer.utxo.repository;

import com.bloxbean.cardano.yaci.indexer.utxo.model.InvalidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidTransactionRepository extends JpaRepository<InvalidTransaction, String> {
    int deleteBySlotGreaterThan(long slot);
}

