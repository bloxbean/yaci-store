package com.bloxbean.cardano.yaci.indexer.script.repository;

import com.bloxbean.cardano.yaci.indexer.script.model.TxScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxScriptRepository extends JpaRepository<TxScript, Long> {
    List<TxScript> findByTxHash(String txHash);
}
