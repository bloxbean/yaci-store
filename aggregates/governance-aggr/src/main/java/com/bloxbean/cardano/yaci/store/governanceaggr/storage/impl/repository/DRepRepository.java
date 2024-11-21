package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DRepRepository extends JpaRepository<DRepEntity, String> {
    @Query("SELECT d FROM DRepEntity d WHERE d.drepId =:dRepId AND d.epoch <= :epoch ORDER BY d.slot DESC, d.txIndex DESC, d.certIndex DESC limit 1")
    Optional<DRepEntity> findRecentByDRepIdAndEpoch(String dRepId, Integer epoch);

    int deleteBySlotGreaterThan(long slot);
}
