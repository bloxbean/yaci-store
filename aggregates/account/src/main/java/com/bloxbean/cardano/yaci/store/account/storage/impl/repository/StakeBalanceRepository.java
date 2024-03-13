package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StakeBalanceRepository extends JpaRepository<StakeAddressBalanceEntity, StakeAddressBalanceId> {

    Optional<StakeAddressBalanceEntity> findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(String address, Long slot);
    Optional<StakeAddressBalanceEntity> findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(String address, Long blockTime);

    @Query("SELECT a FROM StakeAddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM StakeAddressBalanceEntity b " +
            "  WHERE a.address = b.address" +
            ")")
    Optional<StakeAddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    @Modifying
    @Query("DELETE FROM StakeAddressBalanceEntity sb " +
            "WHERE sb.address = :address " +
            "AND sb.slot < :slot")
    int deleteAllBeforeSlot(String address, Long slot);

    int deleteBySlotGreaterThan(Long slot);
    int deleteByBlockNumberGreaterThan(Long block);
}
