package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardId;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, RewardId> {

    @Modifying
    @Query("delete from RewardEntity r where r.earnedEpoch = :earnedEpoch and (r.type = 'leader' or r.type = 'member')")
    int deleteLeaderMemberRewards(int earnedEpoch);

    @Query("select max(r.earnedEpoch) from RewardEntity r where r.type = :rewardType")
    Optional<Integer> getLastRewardCalculationEpoch(RewardType rewardType);
}
