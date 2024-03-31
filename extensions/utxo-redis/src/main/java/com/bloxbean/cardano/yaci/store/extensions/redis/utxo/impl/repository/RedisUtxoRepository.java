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

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerAddr(String ownerAddress);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerAddrAndSpentTxHashIsNull(String ownerAddress, Pageable page);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerStakeAddr(String ownerStakeAddress);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerStakeAddrAndSpentTxHashIsNull(String ownerStakeAddress, Pageable page);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerStakeAddrAndAmounts_Unit(String ownerStakeAddress, String unit);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerStakeAddrAndSpentTxHashIsNullAndAmounts_Unit(String ownerStakeAddress, Pageable page);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerPaymentCredential(String paymentKeyHash);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerPaymentCredentialAndSpentTxHashIsNull(String paymentKeyHash, Pageable page);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerPaymentCredentialAndAmounts_Unit(String paymentKeyHash, String unit);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerPaymentCredentialAndSpentTxHashIsNullAndAmounts_Unit(String paymentKeyHash, String unit, Pageable page);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findBySlotGreaterThan(Long slot);

    List<RedisAddressUtxoEntity> findBySlotGreaterThanAndSpentTxHashIsNull(Long slot);

    List<RedisAddressUtxoEntity> findBySlotGreaterThanAndSpentTxHashIsNotNull(Long slot);

    Page<RedisAddressUtxoEntity> findDistinctByBlockNumberGreaterThanEqualOrderByBlockNumberAsc(Long blockNumber, Pageable pageable);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    //Find unspent between blocks
    List<RedisAddressUtxoEntity> findByBlockNumberBetween(Long startBlock, Long endBlock);

    Optional<List<RedisAddressUtxoEntity>> findByBlockNumberBetweenAndSpentTxHashIsNull(Long startBlock, Long endBlock);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByOwnerAddrAndAmounts_Unit(String address, String unit);

    Optional<List<RedisAddressUtxoEntity>> findByOwnerAddrAndSpentTxHashIsNullAndAmounts_Unit(String address, String unit);

    // TODO Remove After Bug Fix https://github.com/redis/redis-om-spring/issues/399
    List<RedisAddressUtxoEntity> findByAmounts_Unit(String unit);

    List<RedisAddressUtxoEntity> findBySpentTxHashIsNullAndAmounts_Unit(String unit);

    List<RedisAddressUtxoEntity> findBySpentAtBlockLessThan(Long block);

    List<RedisAddressUtxoEntity> findBySpentAtBlockBetween(Long start, Long end);
}

