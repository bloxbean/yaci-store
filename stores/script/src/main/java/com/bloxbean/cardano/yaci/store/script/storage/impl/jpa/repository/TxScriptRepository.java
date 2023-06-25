package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.TxScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxScriptRepository extends JpaRepository<TxScriptEntity, Long> {
    List<TxScriptEntity> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
