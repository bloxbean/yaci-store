package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.RollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RollbackJpaRepository extends JpaRepository<RollbackEntity, Long> {
}
