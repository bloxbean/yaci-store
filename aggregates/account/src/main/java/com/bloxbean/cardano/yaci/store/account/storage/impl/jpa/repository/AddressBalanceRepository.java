package com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.jpa.model.AddressBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressBalanceRepository extends JpaRepository<AddressBalanceEntity, AddressBalanceId> {

    Optional<AddressBalanceEntity> findTopByAddressAndUnitAndSlotIsLessThanEqualOrderBySlotDesc(String address, String unit, Long slot);

    @Query("SELECT a FROM AddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM AddressBalanceEntity b " +
            "  WHERE a.address = b.address " +
            "  AND a.unit = b.unit" +
            ")")
    List<AddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    int deleteBySlotGreaterThan(Long slot);
}
