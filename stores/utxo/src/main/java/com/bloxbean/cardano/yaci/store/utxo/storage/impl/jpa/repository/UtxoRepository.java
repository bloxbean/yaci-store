package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtxoRepository extends JpaRepository<AddressUtxoEntity, UtxoId> {
    Optional<List<AddressUtxoEntity>> findByOwnerAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);
    Optional<List<AddressUtxoEntity>> findByOwnerStakeAddrAndSpent(String ownerAddress, Boolean spent, Pageable page);


    Optional<List<AddressUtxoEntity>> findByOwnerPaymentCredentialAndSpent(String paymentKeyHash, Boolean spent, Pageable page);

    Optional<List<AddressUtxoEntity>> findByOwnerStakeCredentialAndSpent(String paymentKeyHash, Boolean spent, Pageable page);

    List<AddressUtxoEntity> findAllById(Iterable<UtxoId> utxoIds);

    int deleteBySlotGreaterThan(Long slot);

    //Required for account balance aggregation
    @Query("SELECT distinct ab.blockNumber FROM AddressUtxoEntity  ab where ab.blockNumber >= :block order by ab.blockNumber ASC LIMIT :limit")
    List<Long> findNextAvailableBlocks(Long block, int limit);

    List<AddressUtxoEntity> findByBlockNumberBetween(Long startBlock, Long endBlock);
    List<AddressUtxoEntity> findBySpentAtBlockBetween(Long startBlock, Long endBlock);

    //The followings are not used currently
    @Query("SELECT MIN(ab.blockNumber) FROM AddressUtxoEntity ab WHERE ab.blockNumber > :block")
    Long findNextAvailableBlock(Long block);

    @Query("SELECT MAX(ab.blockNumber) FROM AddressUtxoEntity ab")
    Long findMaxBlockNumer();

    List<AddressUtxoEntity> findBySlot(Long slot);
    List<AddressUtxoEntity> findBySpentAtSlot(Long slot);
    List<AddressUtxoEntity> findByBlockNumber(Long blockNumber);
    List<AddressUtxoEntity> findBySpentAtBlock(Long slot);
}

