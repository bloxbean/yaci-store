package com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, String> {

    int deleteBySlotGreaterThan(Long slot);

    //Optional - Read queries
    Optional<BlockEntity> findTopByOrderByNumberDesc();

    Optional<BlockEntity> findByHash(String hash);

    Optional<BlockEntity> findByNumber(Long number);

    List<BlockEntity> findByEpochNumber(int epochNumber);

    @Query("select b from BlockEntity b")
    Slice<BlockEntity> findAllBlocks(Pageable pageable);

    List<BlockEntity> getBlockEntitiesBySlotLeaderAndEpochNumber(String slotLeader, int epochNumber);

    @Query("select count(b.number) from BlockEntity b where b.epochNumber = :epochNumber")
    int totalBlocksInEpoch(int epochNumber);


    Page<BlockEntity> findByEpochNumber(int epochNumber, Pageable pageable);
}

