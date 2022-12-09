package com.bloxbean.cardano.yaci.indexer.blocks.repository;

import com.bloxbean.cardano.yaci.indexer.blocks.model.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockRepository extends PagingAndSortingRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByBlockDesc();

    Optional<BlockEntity> findByBlockHash(String blockHash);

    Optional<BlockEntity> findByBlock(long block);

    int deleteBySlotGreaterThan(long slot);
}

