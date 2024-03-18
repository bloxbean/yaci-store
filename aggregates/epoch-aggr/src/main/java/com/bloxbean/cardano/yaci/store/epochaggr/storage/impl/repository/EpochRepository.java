package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository(value = "epochRepositoryJpa")
public interface EpochRepository extends JpaRepository<EpochEntityJpa, Long> {
    Optional<EpochEntityJpa> findTopByOrderByNumberDesc();
}
