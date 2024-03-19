package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpochRepository extends JpaRepository<EpochEntity, Long> {
    Optional<EpochEntity> findTopByOrderByNumberDesc();
}
