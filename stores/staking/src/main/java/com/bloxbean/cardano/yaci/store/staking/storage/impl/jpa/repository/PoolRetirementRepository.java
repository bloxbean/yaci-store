package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRetirementEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRetirementId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRetirementRepository
        extends JpaRepository<PoolRetirementEntity, PoolRetirementId> {

    @Query("select p from PoolRetirementEntity p")
    Slice<PoolRetirementEntity> findAllPools(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
