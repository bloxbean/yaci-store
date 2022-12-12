package com.bloxbean.cardano.yaci.store.blocks.repository;

import com.bloxbean.cardano.yaci.store.blocks.model.BlockEntity;
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

