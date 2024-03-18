package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntityJpa, String> {

    Optional<TxnEntityJpa> findByTxHash(String txHash);
    List<TxnEntityJpa> findAllByBlockHash(String blockHash);
    List<TxnEntityJpa> findAllByBlockNumber(Long blockNumber);

    int deleteBySlotGreaterThan(Long slot);
}
