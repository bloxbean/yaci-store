package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.AdaPotEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.AdaPotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdaPotRepository extends JpaRepository<AdaPotEntity, AdaPotId> {

    @Query("SELECT a FROM AdaPotEntity a WHERE a.epoch = :epoch " +
            "AND a.slot = (SELECT MAX(a.slot) FROM AdaPotEntity a WHERE a.epoch = :epoch) ORDER BY a.epochBoundary asc limit 1")
    Optional<AdaPotEntity> findByEpoch(Long epoch);

    int deleteBySlotGreaterThan(Long slot);
}
