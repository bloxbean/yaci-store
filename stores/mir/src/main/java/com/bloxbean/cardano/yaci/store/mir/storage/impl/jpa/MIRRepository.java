package com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.model.MIREntity;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.projection.MIRSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MIRRepository
        extends JpaRepository<MIREntity, Long> {

    List<MIREntity> findByTxHash(String txHash);

    int deleteBySlotGreaterThan(Long slot);

    @Query("SELECT m.txHash as txHash, m.pot as pot, m.certIndex as certIndex, m.slot as slot, m.blockNumber as blockNumber, m.blockTime as blockTime, COUNT(*) as totalStakeKeys, SUM(m.amount) AS totalRewards " +
            "FROM MIREntity m " +
            "GROUP BY m.txHash, m.pot, m.certIndex, m.slot, m.blockNumber, m.blockTime " +
            "ORDER BY m.slot DESC")
    Page<MIRSummary> findRecentMIRSummaries(Pageable pageable);
}
