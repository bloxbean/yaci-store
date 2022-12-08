package com.bloxbean.cardano.yaci.indexer.blocks.repository;

import com.bloxbean.cardano.yaci.indexer.blocks.model.RollbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RollbackRepository extends JpaRepository<RollbackEntity, Long> {
}
