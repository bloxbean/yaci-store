package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaRollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRollbackRepository extends JpaRepository<JpaRollbackEntity, Long> {
}
