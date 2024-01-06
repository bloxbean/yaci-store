package com.bloxbean.cardano.yaci.store.account.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressBalanceRepository extends JpaRepository<AddressBalanceEntity, AddressBalanceId> {

    Optional<AddressBalanceEntity> findTopByAddressAndSlotIsLessThanEqualOrderBySlotDesc(String address, Long slot);

    Optional<AddressBalanceEntity> findTopByAddressAndBlockTimeIsLessThanEqualOrderByBlockTimeDesc(String address, Long blockTime);

    @Query("SELECT a FROM AddressBalanceEntity a " +
            "WHERE a.address = :address " +
            "AND a.slot = (" +
            "  SELECT MAX(b.slot) FROM AddressBalanceEntity b " +
            "  WHERE a.address = b.address " +
            ")")
    List<AddressBalanceEntity> findLatestAddressBalanceByAddress(String address);

    int deleteByAddressAndSlotLessThan(String address, Long slot);

    int deleteBySlotGreaterThan(Long slot);

    int deleteByBlockNumberGreaterThan(Long block);

    @Query("select MAX (ab.blockNumber) from AddressBalanceEntity ab")
    Long findMaxBlock();
}
