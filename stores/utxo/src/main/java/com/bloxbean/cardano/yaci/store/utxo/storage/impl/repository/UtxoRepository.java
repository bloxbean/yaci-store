package com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxoEntity, UtxoId> {

    @Query("SELECT a FROM AddressUtxoEntity a LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerAddr = :ownerAddress AND s.txHash IS NULL")
    Optional<List<AddressUtxoEntity>> findUnspentByOwnerAddr(String ownerAddress, Pageable page);

    @Query("SELECT a FROM AddressUtxoEntity a LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerStakeAddr = :ownerAddress AND s.txHash IS NULL")
    Optional<List<AddressUtxoEntity>> findUnspentByOwnerStakeAddr(String ownerAddress, Pageable page);


    @Query("SELECT a FROM AddressUtxoEntity a LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerPaymentCredential = :paymentKeyHash AND s.txHash IS NULL")
    Optional<List<AddressUtxoEntity>> findUnspentByOwnerPaymentCredential(String paymentKeyHash, Pageable page);

    @Query("SELECT a FROM AddressUtxoEntity a LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
            "WHERE a.ownerStakeCredential = :delegationHash AND s.txHash IS NULL")
    Optional<List<AddressUtxoEntity>> findUnspentByOwnerStakeCredential(String delegationHash, Pageable page);

    List<AddressUtxoEntity> findAllById(Iterable<UtxoId> utxoIds);

    @Query("""
             SELECT a FROM AddressUtxoEntity a 
             LEFT JOIN AmtEntity amt ON a.txHash = amt.txHash AND a.outputIndex = amt.outputIndex           
             LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex 
             WHERE  amt.unit = :unit AND s.txHash IS NULL"""
    )
    Stream<AddressUtxoEntity> findByAssetUnit(String unit, Pageable page);

    @Query("""
             SELECT a FROM AddressUtxoEntity a 
             LEFT JOIN AmtEntity amt ON a.txHash = amt.txHash AND a.outputIndex = amt.outputIndex           
             LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex 
             WHERE  a.ownerAddr = :address and amt.unit = :unit AND s.txHash IS NULL"""
    )
    Stream<AddressUtxoEntity> findByAddressAndAssetUnit(String address, String unit, Pageable page);

    @Query("""
             SELECT a FROM AddressUtxoEntity a 
             LEFT JOIN AmtEntity amt ON a.txHash = amt.txHash AND a.outputIndex = amt.outputIndex           
             LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex 
             WHERE  a.ownerPaymentCredential = :paymentKeyHash and amt.unit = :unit AND s.txHash IS NULL"""
    )
    Stream<AddressUtxoEntity> findByOwnerPaymentCredentialAndAssetUnit(String paymentKeyHash, String unit, Pageable page);

    @Query("""
             SELECT a FROM AddressUtxoEntity a 
             LEFT JOIN AmtEntity amt ON a.txHash = amt.txHash AND a.outputIndex = amt.outputIndex           
             LEFT JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex 
             WHERE  a.ownerStakeAddr = :stakeAddress and amt.unit = :unit AND s.txHash IS NULL"""
    )
    Stream<AddressUtxoEntity> findByOwnerStakeAddressAndAssetUnit(String stakeAddress, String unit, Pageable page);

    int deleteBySlotGreaterThan(Long slot);

    @Modifying
    @Transactional
    @Query("DELETE FROM AddressUtxoEntity a WHERE a IN (SELECT au FROM AddressUtxoEntity au JOIN TxInputEntity s ON " +
            "au.txHash = s.txHash AND au.outputIndex = s.outputIndex AND s.spentAtBlock < :block)")
    int deleteBySpentAndBlockLessThan(Long block);
}

