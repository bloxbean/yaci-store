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

    @Query("select sum(e.amount) from EpochStakeEntity e where e.epoch = :activeEpoch - 2")
    Optional<BigInteger> getTotalActiveStakeForEpoch(Integer activeEpoch);

    @Query("select e from EpochStakeEntity e where e.address = :address and e.epoch = :activeEpoch - 2")
    Optional<EpochStakeEntity> findByAddressAndActiveEpoch(String address, Integer activeEpoch);

    @Query("select sum(e.amount) from EpochStakeEntity e where e.epoch = :activeEpoch - 2 and e.poolId = :poolId")
    Optional<BigInteger> getActiveStakeByPoolAndEpoch(Integer activeEpoch, String poolId);

    @Query("select e from EpochStakeEntity e where e.epoch = :activeEpoch - 2")
    List<EpochStakeEntity> getAllActiveStakesByEpoch(Integer activeEpoch, Pageable pageable);

    @Query("select e from EpochStakeEntity e where e.epoch = :activeEpoch - 2 and e.poolId = :poolId")
    List<EpochStakeEntity> getAllByActiveEpochAndPool(Integer activeEpoch, String poolId, Pageable pageable);

    @Query("select e from EpochStakeEntity e where e.epoch = :activeEpoch - 2 and e.poolId in :poolIds")
    List<EpochStakeEntity> getAllByActiveEpochAndPools(Integer activeEpoch, List<String> poolIds);
}