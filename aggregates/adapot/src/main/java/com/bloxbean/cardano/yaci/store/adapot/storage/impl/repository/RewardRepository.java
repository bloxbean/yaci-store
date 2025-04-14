package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, RewardId> {

    @Modifying
    @Query("delete from RewardEntity r where r.earnedEpoch = :earnedEpoch and (r.type = 'leader' or r.type = 'member')")
    int deleteLeaderMemberRewards(int earnedEpoch);

    Slice<RewardEntity> findByEarnedEpoch(Integer epoch, Pageable pageable);
    Slice<RewardEntity> findBySpendableEpoch(Integer epoch, Pageable pageable);

    List<RewardEntity> findByAddressAndEarnedEpoch(String address, Integer earnedEpoch);
    List<RewardEntity> findByAddressAndSpendableEpoch(String address, Integer spendableEpoch);
    Slice<RewardEntity> findByAddress(String address, Pageable pageable);

    Slice<RewardEntity> findByPoolIdAndSpendableEpoch(String poolId, Integer spendableEpoch, Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
