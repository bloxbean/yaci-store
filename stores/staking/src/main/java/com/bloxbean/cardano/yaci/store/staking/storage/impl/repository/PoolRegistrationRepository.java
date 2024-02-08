package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolRegistrationRepository
        extends JpaRepository<PoolRegistrationEnity, PoolRegistrationId> {

    @Query("select p from PoolRegistrationEnity p")
    Slice<PoolRegistrationEnity> findAllPools(Pageable pageable);


    @Query("select distinct p.poolId from PoolRegistrationEnity p where p.epoch <= ?1")
    List<String> findAllRegisteredPool(Integer epoch);

    @Query("select p from PoolRegistrationEnity p where p.poolId = ?1 and p.epoch <= ?2 order by p.slot desc, p.certIndex desc limit 1")
    Optional<PoolRegistrationEnity> findRecentPoolRegistrationByEpoch(String poolId, Integer epoch);

    List<PoolRegistrationEnity> findByPoolIdAndEpochLessThanEqual(String poolId, Integer epoch);

    int deleteBySlotGreaterThan(Long slot);
}
