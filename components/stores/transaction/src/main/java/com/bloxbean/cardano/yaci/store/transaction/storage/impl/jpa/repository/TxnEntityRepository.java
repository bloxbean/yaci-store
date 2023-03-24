package com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.model.TxnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntity, String> {

    Optional<TxnEntity> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
