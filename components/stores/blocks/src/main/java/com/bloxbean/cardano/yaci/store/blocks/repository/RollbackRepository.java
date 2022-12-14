package com.bloxbean.cardano.yaci.store.blocks.repository;

import com.bloxbean.cardano.yaci.store.blocks.model.RollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RollbackRepository extends JpaRepository<RollbackEntity, Long> {
}
