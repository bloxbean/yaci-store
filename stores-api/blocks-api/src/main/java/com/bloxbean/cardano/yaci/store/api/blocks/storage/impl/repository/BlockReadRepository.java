package com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.BlockEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockReadRepository extends JpaRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByNumberDesc();

    Optional<BlockEntity> findByHash(String hash);

    Optional<BlockEntity> findByNumber(Long number);

    List<BlockEntity> findByEpochNumber(int epochNumber);

    @Query("select b from BlockEntity b")
    Slice<BlockEntity> findAllBlocks(Pageable pageable);

    List<BlockEntity> getBlockEntitiesBySlotLeaderAndEpochNumber(String slotLeader, int epochNumber);

}

