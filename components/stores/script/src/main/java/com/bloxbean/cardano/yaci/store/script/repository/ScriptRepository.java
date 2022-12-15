package com.bloxbean.cardano.yaci.store.script.repository;

import com.bloxbean.cardano.yaci.store.script.model.ScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptRepository extends JpaRepository<ScriptEntity, String> {
}
