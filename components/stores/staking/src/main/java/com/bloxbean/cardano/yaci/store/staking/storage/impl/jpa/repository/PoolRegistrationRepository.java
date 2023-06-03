package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRegistrationRepository
        extends JpaRepository<PoolRegistrationEnity, PoolRegistrationId> {

    int deleteBySlotGreaterThan(Long slot);
}
