package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.EraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EraRepository extends JpaRepository<EraEntity, Integer> {

    @Query("select e from EraEntity e where e.era > 1 order by e.era asc limit 1")
    Optional<EraEntity> findFirstNonByronEra();

    @Query("select e from EraEntity e where e.era = (select max(ee.era) from EraEntity ee)")
    Optional<EraEntity> findCurrentEra();

    @Query("select e from EraEntity e order by e.era asc")
    List<EraEntity> findAllEras();
}
