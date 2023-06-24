package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeRegistrationRepository
        extends JpaRepository<StakeRegistrationEntity, StakeRegistrationId> {

    @Query("select r from StakeRegistrationEntity r where r.type = 'STAKE_REGISTRATION'")
    Slice<StakeRegistrationEntity> findRegistrations(Pageable pageable);

    @Query("select r from StakeRegistrationEntity r where r.type = 'STAKE_DEREGISTRATION'")
    Slice<StakeRegistrationEntity> findDeregestrations(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
