package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRegistrationRepository
        extends JpaRepository<PoolRegistrationEnity, PoolRegistrationId> {

    @Query("select p from PoolRegistrationEnity p")
    Slice<PoolRegistrationEnity> findAllPools(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
