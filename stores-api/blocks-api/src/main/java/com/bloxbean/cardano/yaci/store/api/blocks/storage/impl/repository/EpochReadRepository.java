package com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.EpochEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpochReadRepository extends JpaRepository<EpochEntity, Long> {

    Optional<EpochEntity> findTopByOrderByNumberDesc();
}
