package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
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
public interface AddressBalanceRepository extends JpaRepository<AddressBalanceEntity, AddressBalanceId> {

    Optional<AddressBalanceEntity> findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(String address, String unit, Long slot);
    List<AddressBalanceEntity> findByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(String address, String unit, Long slot);

    Optional<AddressBalanceEntity> findTopByAddressAndUnitAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(String address, String unit, Long blockTime);

    @Query("SELECT a FROM AddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM AddressBalanceEntity b " +
            "  WHERE a.address = b.address " +
            "  AND a.unit = b.unit" +
            ")")
    List<AddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    @Query("SELECT ab " +
            "FROM AddressBalanceEntity ab " +
            "WHERE ab.unit = :unit and ab.quantity > 0" +
            "GROUP BY ab.address, ab.unit, ab.slot " +
            "HAVING ab.slot = (SELECT MAX(ab2.slot) FROM AddressBalanceEntity ab2 WHERE ab2.address = ab.address AND ab2.unit = ab.unit)")
    Slice<AddressBalanceEntity> findLatestAddressBalanceByUnit(String unit, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AddressBalanceEntity ab " +
            "WHERE ab.address = :address " +
            "AND ab.unit = :unit " +
            "AND ab.slot < :slot")
    int deleteAllBeforeSlot(String address, String unit, Long slot);

    int deleteBySlotGreaterThan(Long slot);
    int deleteByBlockNumberGreaterThan(Long block);
}
