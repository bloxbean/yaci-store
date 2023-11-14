package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    int deleteBySlotGreaterThan(Long slot);

    //Required for account balance aggregation
    @Query("SELECT distinct ab.blockNumber FROM AddressUtxoEntity  ab where ab.blockNumber >= :block order by ab.blockNumber ASC LIMIT :limit")
    List<Long> findNextAvailableBlocks(Long block, int limit);

    //Find unspent between blocks
    List<AddressUtxoEntity> findByBlockNumberBetween(Long startBlock, Long endBlock);


    @Query("SELECT a,s FROM AddressUtxoEntity a JOIN TxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
            "WHERE s.spentAtBlock BETWEEN :startBlock AND :endBlock")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "false"),
            @QueryHint(name = "org.hibernate.readOnly", value = "true") })
    List<Object[]> findBySpentAtBlockBetween(Long startBlock, Long endBlock);

}

