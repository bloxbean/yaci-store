package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntity, String> {

    Optional<TxnEntity> findByTxHash(String txHash);
    List<TxnEntity> findAllByBlockHash(String blockHash);
    List<TxnEntity> findAllByBlockNumber(Long blockNumber);

    int deleteBySlotGreaterThan(Long slot);
}
