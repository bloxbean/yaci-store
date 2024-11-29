package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpochStakeRepository extends JpaRepository<EpochStakeEntity, EpochStakeId> {

    @Query("select max(e.epoch) from EpochStakeEntity e")
    Optional<Integer> getMaxEpoch();

    @Query("select sum(e.amount) from EpochStakeEntity e where e.activeEpoch = :epoch")
    Optional<BigInteger> getTotalActiveStakeForEpoch(Integer epoch);

    Optional<EpochStakeEntity> findByAddressAndActiveEpoch(String address, Integer activeEpoch);

    @Query("select sum(e.amount) from EpochStakeEntity e where e.activeEpoch = :epoch and e.poolId = :poolId")
    Optional<BigInteger> getActiveStakeByPoolAndEpoch(Integer epoch, String poolId);

    @Query("select e from EpochStakeEntity e where e.activeEpoch = :epoch")
    List<EpochStakeEntity> getAllActiveStakesByEpoch(Integer epoch, Pageable pageable);

    @Query("select e from EpochStakeEntity e where e.activeEpoch = :epoch and e.poolId = :poolId")
    List<EpochStakeEntity> getAllByActiveEpochAndPool(Integer epoch, String poolId, Pageable pageable);

    @Query("select e from EpochStakeEntity e where e.activeEpoch = :epoch and e.poolId in :poolIds")
    List<EpochStakeEntity> getAllByActiveEpochAndPools(Integer epoch, List<String> poolIds);
}
