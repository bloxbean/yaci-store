package com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AddressBalanceId;
import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.StakeAddressBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StakeBalanceRepository extends JpaRepository<StakeAddressBalanceEntity, AddressBalanceId> {

    Optional<StakeAddressBalanceEntity> findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(String address, String unit, Long slot);

    @Query("SELECT a FROM StakeAddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM StakeAddressBalanceEntity b " +
            "  WHERE a.address = b.address " +
            "  AND a.unit = b.unit" +
            ")")
    List<StakeAddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    int deleteBySlotGreaterThan(Long slot);
}
