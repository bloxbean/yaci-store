package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRetirementRepository
        extends JpaRepository<PoolRetirementEntityJpa, PoolRetirementId> {

    @Query("select p from PoolRetirementEntityJpa p")
    Slice<PoolRetirementEntityJpa> findAllPools(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
