package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.BlockEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockJpaRepository extends PagingAndSortingRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByBlockDesc();

    Optional<BlockEntity> findByBlockHash(String blockHash);

    Optional<BlockEntity> findByBlock(Long block);

    int deleteBySlotGreaterThan(Long slot);
}

