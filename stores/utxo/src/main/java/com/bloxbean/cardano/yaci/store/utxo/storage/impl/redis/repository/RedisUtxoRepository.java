package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisAddressUtxoEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedisUtxoRepository extends RedisDocumentRepository<RedisAddressUtxoEntity, String> {

//    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
//        "WHERE a.ownerAddr = :ownerAddress AND s.txHash IS NULL")
//    Optional<List<RedisAddressUtxoEntity>> findUnspentByOwnerAddr(String ownerAddress, Pageable page);
//
//    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
//        "WHERE a.ownerStakeAddr = :ownerAddress AND s.txHash IS NULL")
//    Optional<List<RedisAddressUtxoEntity>> findUnspentByOwnerStakeAddr(String ownerAddress, Pageable page);
//
//
//    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
//        "WHERE a.ownerPaymentCredential = :paymentKeyHash AND s.txHash IS NULL")
//    Optional<List<RedisAddressUtxoEntity>> findUnspentByOwnerPaymentCredential(String paymentKeyHash, Pageable page);
//
//    @Query("SELECT a FROM JpaAddressUtxoEntity a LEFT JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
//            "WHERE a.ownerStakeCredential = :delegationHash AND s.txHash IS NULL")
//    Optional<List<RedisAddressUtxoEntity>> findUnspentByOwnerStakeCredential(String delegationHash, Pageable page);

    Integer deleteBySlotGreaterThan(Long slot);

    //Required for account balance aggregation
    Page<RedisAddressUtxoEntity> findDistinctByBlockNumberGreaterThanEqualOrderByBlockNumberAsc(Long blockNumber, Pageable pageable);

    //Find unspent between blocks
    List<RedisAddressUtxoEntity> findByBlockNumberBetween(Long startBlock, Long endBlock);


//    @Query("SELECT a,s FROM JpaAddressUtxoEntity a JOIN JpaTxInputEntity s ON a.txHash = s.txHash AND a.outputIndex = s.outputIndex " +
//            "WHERE s.spentAtBlock BETWEEN :startBlock AND :endBlock")
//    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "false"),
//            @QueryHint(name = "org.hibernate.readOnly", value = "true") })
//    List<Object[]> findBySpentAtBlockBetween(Long startBlock, Long endBlock);
//
//    @Modifying
//    @Transactional
//    @Query("DELETE FROM JpaAddressUtxoEntity a WHERE a IN (SELECT au FROM JpaAddressUtxoEntity au JOIN JpaTxInputEntity s ON " +
//            "au.txHash = s.txHash AND au.outputIndex = s.outputIndex AND s.spentAtBlock < :block)")
//    int deleteBySpentAndBlockLessThan(Long block);
}

