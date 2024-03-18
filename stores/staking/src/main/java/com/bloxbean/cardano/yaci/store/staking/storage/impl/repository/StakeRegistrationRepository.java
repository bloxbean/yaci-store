package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeRegistrationRepository
        extends JpaRepository<StakeRegistrationEntityJpa, StakeRegistrationId> {

    @Query("select r from StakeRegistrationEntityJpa r where r.type = 'STAKE_REGISTRATION'")
    Slice<StakeRegistrationEntityJpa> findRegistrations(Pageable pageable);

    @Query("select r from StakeRegistrationEntityJpa r where r.type = 'STAKE_DEREGISTRATION'")
    Slice<StakeRegistrationEntityJpa> findDeregestrations(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
