package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, UUID> {

    Slice<RewardEntity> findByEarnedEpoch(Long epoch, Pageable pageable);

    Slice<RewardEntity> findByEarnedEpochAndType(Long epoch, RewardType rewardType, Pageable pageable);

    @Query("select max(r.earnedEpoch) from RewardEntity r where r.type = :rewardType")
    Optional<Integer> getLastRewardCalculationEpoch(RewardType rewardType);

    int deleteBySlotGreaterThan(Long slot);
}
