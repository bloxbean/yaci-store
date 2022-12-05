package com.bloxbean.cardano.yaci.indexer.blocks.repository;

import com.bloxbean.cardano.yaci.indexer.blocks.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByBlockDesc();

    Optional<BlockEntity> findByBlockHash(String blockHash);

    int deleteBySlotGreaterThan(long slot);
}

