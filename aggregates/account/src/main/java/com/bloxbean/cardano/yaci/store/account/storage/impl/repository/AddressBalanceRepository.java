package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressBalanceRepository extends JpaRepository<JpaAddressBalanceEntity, AddressBalanceId> {

    Optional<JpaAddressBalanceEntity> findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(String address, String unit, Long slot);

    Optional<JpaAddressBalanceEntity> findTopByAddressAndUnitAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(String address, String unit, Long blockTime);

    @Query("SELECT a FROM JpaAddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM JpaAddressBalanceEntity b " +
            "  WHERE a.address = b.address " +
            "  AND a.unit = b.unit" +
            ")")
    List<JpaAddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    @Query("SELECT ab " +
            "FROM JpaAddressBalanceEntity ab " +
            "WHERE ab.unit = :unit and ab.quantity > 0" +
            "GROUP BY ab.address, ab.unit, ab.slot " +
            "HAVING ab.slot = (SELECT MAX(ab2.slot) FROM JpaAddressBalanceEntity ab2 WHERE ab2.address = ab.address AND ab2.unit = ab.unit)")
    Slice<JpaAddressBalanceEntity> findLatestAddressBalanceByUnit(String unit, Pageable pageable);

    @Modifying
    @Query("DELETE FROM JpaAddressBalanceEntity ab " +
            "WHERE ab.address = :address " +
            "AND ab.unit = :unit " +
            "AND ab.slot < :slot")
    int deleteAllBeforeSlot(String address, String unit, Long slot);

    int deleteBySlotGreaterThan(Long slot);
    int deleteByBlockNumberGreaterThan(Long block);

}
