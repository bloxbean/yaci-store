package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntityJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochParamRepository extends JpaRepository<EpochParamEntityJpa, Integer> {

    @Query("select max(ep.epoch) from EpochParamEntityJpa ep")
    Integer findMaxEpoch();

    int deleteBySlotGreaterThan(Long slot);
}
