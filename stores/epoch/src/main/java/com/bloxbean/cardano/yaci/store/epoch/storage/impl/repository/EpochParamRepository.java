package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpochParamRepository extends JpaRepository<EpochParamEntity, Integer> {

    @Query("select max(ep.epoch) from EpochParamEntity ep")
    Integer findMaxEpoch();

    @Query("select ep from EpochParamEntity ep where ep.epoch = (select max(ep.epoch) from EpochParamEntity ep)")
    Optional<EpochParamEntity> findLatestEpochParam();

    int deleteBySlotGreaterThan(Long slot);
}
