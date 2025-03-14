package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdaPotJobRepository extends JpaRepository<AdaPotJobEntity, Long> {
    List<AdaPotJobEntity> findByTypeAndStatusOrderByEpoch(AdaPotJobType type, AdaPotJobStatus status);
    Optional<AdaPotJobEntity> findByTypeAndEpoch(AdaPotJobType type, int epoch);
    int deleteBySlotGreaterThan(Long slot);
}
