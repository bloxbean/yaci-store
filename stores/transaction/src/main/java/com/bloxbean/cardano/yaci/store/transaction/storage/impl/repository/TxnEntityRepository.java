package com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.TxnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface TxnEntityRepository extends JpaRepository<TxnEntity, String> {

    Optional<TxnEntity> findByTxHash(String txHash);
    List<TxnEntity> findAllByBlockHash(String blockHash);
    List<TxnEntity> findAllByBlockNumber(Long blockNumber);

    @Query("select sum(t.fee) from TxnEntity t where t.epoch = :epoch")
    BigInteger getTotalFee(long epoch);

    @Query("select sum(t.treasuryDonation) from TxnEntity t where t.epoch = :epoch and t.treasuryDonation is not null")
    BigInteger getTotalDonation(long epoch);

    int deleteBySlotGreaterThan(Long slot);
}
