package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaScriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaScriptRepository extends JpaRepository<JpaScriptEntity, String> {
}
