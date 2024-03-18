package com.bloxbean.cardano.yaci.store.script.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxScriptRepository extends JpaRepository<TxScriptEntityJpa, Long> {

    @Query("SELECT t, " +
            "(SELECT d.datum FROM DatumEntityJpa d WHERE d.hash = t.datumHash), " +
            "(SELECT d.datum FROM DatumEntityJpa d WHERE d.hash = t.redeemerDatahash) " +
            "FROM TxScriptEntityJpa t " +
            "WHERE t.txHash = :txHash")
    List<Object[]> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
