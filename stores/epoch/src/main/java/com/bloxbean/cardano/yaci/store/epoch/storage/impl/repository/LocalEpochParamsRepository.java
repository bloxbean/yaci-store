package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.LocalEpochParamsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalEpochParamsRepository extends JpaRepository<LocalEpochParamsEntity, Integer> {

    @Query("SELECT e FROM LocalEpochParamsEntity e ORDER BY e.epoch DESC limit 1")
    Optional<LocalEpochParamsEntity> findLatest();

    @Query("select max(ep.epoch) from LocalEpochParamsEntity ep")
    Optional<Integer> findMaxEpoch();
}
