package com.bloxbean.cardano.yaci.store.mir.storage.impl;

import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.model.MIREntity;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.projection.MIRSummary;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
@Primary
public interface MIRRepository
        extends JpaRepository<MIREntity, Long> {
    int deleteBySlotGreaterThan(Long slot);

    //Read Queries
    List<MIREntity> findByTxHash(String txHash);

    @Query("SELECT m.txHash as txHash, m.pot as pot, m.certIndex as certIndex, m.slot as slot, m.blockNumber as blockNumber, m.blockTime as blockTime, COUNT(*) as totalStakeKeys, SUM(m.amount) AS totalRewards " +
            "FROM MIREntity m " +
            "GROUP BY m.txHash, m.pot, m.certIndex, m.slot, m.blockNumber, m.blockTime " +
            "ORDER BY m.slot DESC")
    Page<MIRSummary> findRecentMIRSummaries(Pageable pageable);

    @Query("SELECT SUM(m.amount) FROM MIREntity m WHERE m.epoch=:epoch and m.pot=:pot")
    BigInteger findMirPotAmountByEpoch(int epoch, MirPot pot);

}
