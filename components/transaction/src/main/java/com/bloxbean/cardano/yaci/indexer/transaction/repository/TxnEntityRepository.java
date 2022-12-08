package com.bloxbean.cardano.yaci.indexer.transaction.repository;

import com.bloxbean.cardano.yaci.indexer.transaction.model.TxnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntity, String> {

    Optional<TxnEntity> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(long slot);
}
