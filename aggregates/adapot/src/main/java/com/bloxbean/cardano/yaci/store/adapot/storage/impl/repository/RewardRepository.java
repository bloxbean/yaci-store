package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, UUID> {

    Slice<RewardEntity> findByEarnedEpoch(Long epoch, Pageable pageable);

    Slice<RewardEntity> findByEarnedEpochAndType(Long epoch, RewardType rewardType, Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
