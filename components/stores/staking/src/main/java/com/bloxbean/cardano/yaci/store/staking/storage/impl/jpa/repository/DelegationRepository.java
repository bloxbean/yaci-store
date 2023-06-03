package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationRepository
        extends JpaRepository<DelegationEntity, DelegationId> {

    int deleteBySlotGreaterThan(Long slot);
}
