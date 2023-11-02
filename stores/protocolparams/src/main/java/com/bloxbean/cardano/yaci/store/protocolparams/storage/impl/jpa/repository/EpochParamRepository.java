package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.EpochParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochParamRepository extends JpaRepository<EpochParamEntity, Integer> {

    @Query("select max(ep.epoch) from EpochParamEntity ep")
    Integer findMaxEpoch();

    int deleteBySlotGreaterThan(Long slot);
}
