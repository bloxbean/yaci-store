package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaBlockEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBlockRepository extends JpaRepository<JpaBlockEntity, String> {

    Integer deleteBySlotGreaterThan(Long slot);

    //Optional - Read queries
    Optional<JpaBlockEntity> findTopByOrderByNumberDesc();

    Optional<JpaBlockEntity> findByHash(String hash);

    Optional<JpaBlockEntity> findByNumber(Long number);

    List<JpaBlockEntity> findByEpochNumber(int epochNumber);

    @Query("select b from JpaBlockEntity b")
    Slice<JpaBlockEntity> findAllBlocks(Pageable pageable);

    List<JpaBlockEntity> getBlockEntitiesBySlotLeaderAndEpochNumber(String slotLeader, int epochNumber);

}

