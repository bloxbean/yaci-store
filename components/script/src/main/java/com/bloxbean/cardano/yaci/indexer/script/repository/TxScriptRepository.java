package com.bloxbean.cardano.yaci.indexer.script.repository;

import com.bloxbean.cardano.yaci.indexer.script.entity.TxScript;
import com.bloxbean.cardano.yaci.indexer.script.entity.TxScriptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TxScriptRepository extends JpaRepository<TxScript, TxScriptId> {
}
