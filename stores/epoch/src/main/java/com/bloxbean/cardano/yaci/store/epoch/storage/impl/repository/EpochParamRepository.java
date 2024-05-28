package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochParamRepository extends JpaRepository<EpochParamEntity, Integer> {

    @Query("select max(ep.epoch) from EpochParamEntity ep")
    Integer findMaxEpoch();

    @Query(
            value =
                    "SELECT ep FROM EpochParamEntity ep WHERE ep.epoch = (SELECT MAX(ep2.epoch) FROM EpochParamEntity ep2)")
    EpochParam findCurrentEpochParam();

    int deleteBySlotGreaterThan(Long slot);
}
