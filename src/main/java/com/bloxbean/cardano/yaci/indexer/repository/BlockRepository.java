package com.bloxbean.cardano.yaci.indexer.repository;

import com.bloxbean.cardano.yaci.indexer.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByBlockDesc();

    Optional<BlockEntity> findByBlockHash(String blockHash);
}

