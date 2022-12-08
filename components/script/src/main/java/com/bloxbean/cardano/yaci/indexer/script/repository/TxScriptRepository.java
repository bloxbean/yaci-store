package com.bloxbean.cardano.yaci.indexer.script.repository;

import com.bloxbean.cardano.yaci.indexer.script.model.TxScript;
import com.bloxbean.cardano.yaci.indexer.script.model.TxScriptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxScriptRepository extends JpaRepository<TxScript, TxScriptId> {
}
