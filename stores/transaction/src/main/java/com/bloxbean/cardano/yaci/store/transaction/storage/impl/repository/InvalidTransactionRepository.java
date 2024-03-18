package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.InvalidTransactionEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidTransactionRepository extends JpaRepository<InvalidTransactionEntityJpa, String> {
    int deleteBySlotGreaterThan(Long slot);
}

