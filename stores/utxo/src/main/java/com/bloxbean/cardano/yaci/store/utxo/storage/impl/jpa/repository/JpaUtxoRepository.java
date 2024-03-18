package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaUtxoId;
import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaUtxoRepository extends JpaRepository<JpaAddressUtxoEntity, JpaUtxoId> {

    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerAddr = :ownerAddress AND s.txHash IS NULL")
    Optional<List<JpaAddressUtxoEntity>> findUnspentByOwnerAddr(String ownerAddress, Pageable page);

    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerStakeAddr = :ownerAddress AND s.txHash IS NULL")
    Optional<List<JpaAddressUtxoEntity>> findUnspentByOwnerStakeAddr(String ownerAddress, Pageable page);


    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
        "WHERE a.ownerPaymentCredential = :paymentKeyHash AND s.txHash IS NULL")
    Optional<List<JpaAddressUtxoEntity>> findUnspentByOwnerPaymentCredential(String paymentKeyHash, Pageable page);

    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
            "WHERE a.ownerStakeCredential = :delegationHash AND s.txHash IS NULL")
    Optional<List<JpaAddressUtxoEntity>> findUnspentByOwnerStakeCredential(String delegationHash, Pageable page);

    List<JpaAddressUtxoEntity> findAllById(Iterable<JpaUtxoId> utxoIds);

    int deleteBySlotGreaterThan(Long slot);

    //Required for account balance aggregation
    @Query("SELECT distinct ab.blockNumber FROM JpaAddressUtxoEntity  ab where ab.blockNumber >= :block order by ab.blockNumber ASC LIMIT :limit")
    List<Long> findNextAvailableBlocks(Long block, int limit);

    //Find unspent between blocks
    List<JpaAddressUtxoEntity> findByBlockNumberBetween(Long startBlock, Long endBlock);


    @Query("SELECT a,s FROM JpaAddressUtxoEntity a JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
            "WHERE s.spentAtBlock BETWEEN :startBlock AND :endBlock")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "false"),
            @QueryHint(name = "org.hibernate.readOnly", value = "true") })
    List<Object[]> findBySpentAtBlockBetween(Long startBlock, Long endBlock);

    @Modifying
    @Transactional
    @Query("DELETE FROM JpaAddressUtxoEntity a WHERE a IN (SELECT au FROM JpaAddressUtxoEntity au JOIN JpaTxInputEntity s ON " +
            "au.txHash = s.txHash AND au.outputIndex = s.outputIndex AND s.spentAtBlock < :block)")
    int deleteBySpentAndBlockLessThan(Long block);
}

