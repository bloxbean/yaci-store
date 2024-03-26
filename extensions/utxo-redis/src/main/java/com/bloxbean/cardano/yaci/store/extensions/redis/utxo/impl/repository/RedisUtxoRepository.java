package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisAddressUtxoEntity;
import com.redis.om.spring.repository.RedisDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedisUtxoRepository extends RedisDocumentRepository<RedisAddressUtxoEntity, String> {

    // TODO Remove After Bug Fix
    List<RedisAddressUtxoEntity> findByOwnerAddr(String ownerAddress);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerAddrAndTxHashIsNull(String ownerAddress, Pageable page);

    // TODO Remove After Bug Fix
    List<RedisAddressUtxoEntity> findByOwnerStakeAddr(String ownerStakeAddress);

    // TODO Remove After Bug Fix
    List<RedisAddressUtxoEntity> findByOwnerStakeAddrAndAmounts_Unit(String ownerStakeAddress, String unit);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerStakeAddrAndTxHashIsNull(String ownerAddress, Pageable page);

    // TODO Remove After Bug Fix
    List<RedisAddressUtxoEntity> findByOwnerPaymentCredential(String paymentKeyHash);

    List<RedisAddressUtxoEntity> findByOwnerPaymentCredentialAndAmounts_Unit(String paymentKeyHash, String unit);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerPaymentCredentialAndTxHashIsNull(String paymentKeyHash, Pageable page);
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


    Integer deleteByTxHashAndOutputIndex(String txHash, long outputIndex);

    List<RedisAddressUtxoEntity> findByOwnerAddrAndAmounts_Unit(String address, String unit);

    List<RedisAddressUtxoEntity> findByAmounts_Unit(String unit);
}

