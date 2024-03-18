package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaStakeAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StakeBalanceRepository extends JpaRepository<JpaStakeAddressBalanceEntity, StakeAddressBalanceId> {

    Optional<JpaStakeAddressBalanceEntity> findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(String address, Long slot);
    Optional<JpaStakeAddressBalanceEntity> findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(String address, Long blockTime);

    @Query("SELECT a FROM JpaStakeAddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM JpaStakeAddressBalanceEntity b " +
            "  WHERE a.address = b.address" +
            ")")
    Optional<JpaStakeAddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    @Modifying
    @Query("DELETE FROM JpaStakeAddressBalanceEntity sb " +
            "WHERE sb.address = :address " +
            "AND sb.slot < :slot")
    int deleteAllBeforeSlot(String address, Long slot);

    int deleteBySlotGreaterThan(Long slot);
    int deleteByBlockNumberGreaterThan(Long block);
}
