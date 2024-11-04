package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StakeRegistrationRepository
        extends JpaRepository<StakeRegistrationEntity, StakeRegistrationId> {

    @Query("select r from StakeRegistrationEntity r where r.type = 'STAKE_REGISTRATION'")
    Slice<StakeRegistrationEntity> findRegistrations(Pageable pageable);

    @Query("select r from StakeRegistrationEntity r where r.type = 'STAKE_DEREGISTRATION'")
    Slice<StakeRegistrationEntity> findDeregestrations(Pageable pageable);

    @Query("select sr.address FROM StakeRegistrationEntity sr WHERE sr.type = 'STAKE_REGISTRATION' " +
            "AND sr.epoch <= :epoch AND NOT EXISTS (SELECT 1 FROM StakeRegistrationEntity sd " +
            "WHERE sd.address = sr.address AND sd.type = 'STAKE_DEREGISTRATION' AND sd.epoch <= sr.epoch AND sd.slot > sr.slot)")
    Slice<String> findRegisteredStakeAddresses(Integer epoch, Pageable pageable);

    @Query("select r from StakeRegistrationEntity r " +
            "where r.address = :stakeAddress and r.slot <= :slot order by r.slot desc, r.txIndex desc, r.certIndex desc limit 1")
    Optional<StakeRegistrationEntity> findRegistrationsByStakeAddress(String stakeAddress, Long slot);

    @Query("select r from StakeRegistrationEntity r " +
            "where r.slot = :slot and r.txIndex = :txIndex and r.certIndex = :certIndex and r.type = 'STAKE_REGISTRATION'")
    Optional<StakeRegistrationEntity> findRegistrationByPointer(Long slot, int txIndex, int certIndex);

    int deleteBySlotGreaterThan(Long slot);
}
