package com.bloxbean.cardano.yaci.store.utxo.repository;

import com.bloxbean.cardano.yaci.store.utxo.model.InvalidTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidTransactionRepository extends JpaRepository<InvalidTransactionEntity, String> {
    int deleteBySlotGreaterThan(Long slot);
}

