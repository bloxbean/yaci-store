package com.bloxbean.cardano.yaci.store.assets.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntityJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface TxAssetRepository extends JpaRepository<TxAssetEntityJpa, Long> {
    int deleteBySlotGreaterThan(Long slot);

    //Optional -- Read queries
    List<TxAssetEntityJpa> findByTxHash(String txHash);

    Slice<TxAssetEntityJpa> findByFingerprint(String fingerprint, Pageable page);

    Slice<TxAssetEntityJpa> findByPolicy(String policy, Pageable page);

    Slice<TxAssetEntityJpa> findByUnit(String unit, Pageable page);

    @Query("select sum(ta.quantity) from TxAssetEntityJpa ta where ta.fingerprint = ?1")
    Optional<BigInteger> getSupplyByFingerprint(String fingerprint);

    @Query("select sum(ta.quantity) from TxAssetEntityJpa ta where ta.unit = ?1")
    Optional<BigInteger> getSupplyByUnit(String unit);

    @Query("select sum(ta.quantity) from TxAssetEntityJpa ta where ta.policy = ?1")
    Optional<BigInteger> getSupplyByPolicy(String policy);
}
