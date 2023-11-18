package com.bloxbean.cardano.yaci.store.script.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.model.ScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRepository extends JpaRepository<ScriptEntity, String> {
}
