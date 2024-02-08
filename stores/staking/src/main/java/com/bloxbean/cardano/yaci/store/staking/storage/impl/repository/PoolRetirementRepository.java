package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolRetirementRepository
        extends JpaRepository<PoolRetirementEntity, PoolRetirementId> {

    @Query("select p from PoolRetirementEntity p")
    Slice<PoolRetirementEntity> findAllPools(Pageable pageable);

    List<PoolRetirementEntity> findByPoolIdAndEpoch(String poolId, Integer epoch);

    //Get all retirement certificates for the pool submitted on or before this epoch
    @Query("select p from PoolRetirementEntity p where p.poolId = ?1 and p.epoch <= ?2 order by p.slot desc, p.certIndex desc limit 1")
    Optional<PoolRetirementEntity> findRecentPoolRetirementByEpoch(String poolId, Integer epoch);

    //Get all retirement certificates with retirement epoch == retirementEpoch
    List<PoolRetirementEntity> findByRetirementEpoch(Integer retirementEpoch);

    List<PoolRetirementEntity> findByPoolIdAndRetirementEpochLessThan(String poolId, Integer retirementEpoch);

    int deleteBySlotGreaterThan(Long slot);
}
