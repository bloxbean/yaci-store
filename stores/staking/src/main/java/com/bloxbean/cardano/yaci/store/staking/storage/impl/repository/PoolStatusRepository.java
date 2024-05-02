package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolStatusRepository extends JpaRepository<PoolEntity, PoolId> {

    @Query("SELECT d FROM PoolEntity d WHERE d.poolId = :poolId AND d.status = :status AND d.epoch <= :epoch ORDER BY d.slot DESC, d.certIndex DESC limit 1")
    Optional<PoolEntity> findRecentByPoolIdAndStatus(String poolId, PoolStatusType status, Integer epoch);

    @Query("SELECT ps FROM PoolEntity ps WHERE ps.status = 'RETIRING' AND ps.retireEpoch = :retireEpoch AND NOT EXISTS (" +
            "SELECT ps2 FROM PoolEntity ps2 WHERE ps2.poolId = ps.poolId AND (ps2.status = 'RETIRING' OR ps2.status = 'UPDATE') " +
            "AND (ps2.slot > ps.slot OR (ps2.slot = ps.slot AND ps2.certIndex > ps.certIndex)))")
    List<PoolEntity> findRetiringPoolsByRetireEpoch(Integer retireEpoch);

    @Query("SELECT ps FROM PoolEntity ps WHERE ps.poolId = :poolId AND ps.status = 'RETIRING' AND ps.retireEpoch = :retireEpoch AND NOT EXISTS (" +
            "SELECT ps2 FROM PoolEntity ps2 WHERE ps2.poolId = ps.poolId AND (ps2.status = 'RETIRING' OR ps2.status = 'UPDATE') " +
            "AND (ps2.slot > ps.slot OR (ps2.slot = ps.slot AND ps2.certIndex > ps.certIndex)))")
    Optional<PoolEntity> findRecentPoolRetirement(String poolId, Integer retireEpoch);

    @Query("SELECT MAX(d.epoch) FROM PoolEntity d")
    Integer getMaxEpoch();

    int deleteBySlotGreaterThan(Long slot);
}
