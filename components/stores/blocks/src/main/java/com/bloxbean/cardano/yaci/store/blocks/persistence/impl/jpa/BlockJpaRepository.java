package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockJpaRepository extends JpaRepository<BlockEntity, String> {

    Optional<BlockEntity> findTopByOrderByNumberDesc();

    Optional<BlockEntity> findByHash(String hash);

    Optional<BlockEntity> findByNumber(Long number);

    int deleteBySlotGreaterThan(Long slot);
}

