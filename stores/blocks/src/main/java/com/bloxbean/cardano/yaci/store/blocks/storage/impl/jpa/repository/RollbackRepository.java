package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.RollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository(value = "rollbackRepositoryJpa")
public interface RollbackRepository extends JpaRepository<RollbackEntity, Long> {
}
