package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaTxScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTxScriptRepository extends JpaRepository<JpaTxScriptEntity, Long> {

    @Query("SELECT t, " +
            "(SELECT d.datum FROM JpaDatumEntity d WHERE d.hash = t.datumHash), " +
            "(SELECT d.datum FROM JpaDatumEntity d WHERE d.hash = t.redeemerDatahash) " +
            "FROM JpaTxScriptEntity t " +
            "WHERE t.txHash = :txHash")
    List<Object[]> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);
}
