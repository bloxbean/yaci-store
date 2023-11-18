package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnWitnessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxnWitnessRepository extends JpaRepository<TxnWitnessEntity, String> {
    List<TxnWitnessEntity> findByTxHash(String txHash);
    int deleteBySlotGreaterThan(Long slot);
}
