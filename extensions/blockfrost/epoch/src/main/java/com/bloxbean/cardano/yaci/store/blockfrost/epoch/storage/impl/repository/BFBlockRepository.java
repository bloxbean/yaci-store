package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BFBlockRepository extends JpaRepository<BlockEntity, String> {

    @Query("select b.hash from BlockEntity b where b.epochNumber = :epoch")
    List<String> findBlockHashesByEpoch(@Param("epoch") int epoch, Pageable pageable);

    @Query("select b.hash from BlockEntity b where b.epochNumber = :epoch and b.slotLeader = :poolId")
    List<String> findBlockHashesByEpochAndPool(@Param("epoch") int epoch, @Param("poolId") String poolId, Pageable pageable);

}
