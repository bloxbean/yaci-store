package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRetirementEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRetirementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRetirementRepository
        extends JpaRepository<PoolRetirementEntity, PoolRetirementId> {

    int deleteBySlotGreaterThan(Long slot);
}
