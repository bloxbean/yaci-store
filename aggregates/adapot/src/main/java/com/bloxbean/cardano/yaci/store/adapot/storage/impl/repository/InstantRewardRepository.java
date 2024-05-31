package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.InstantRewardEntity;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstantRewardRepository extends JpaRepository<InstantRewardEntity, UUID> {

    Slice<InstantRewardEntity> findByEarnedEpoch(Long epoch, Pageable pageable);

    Slice<InstantRewardEntity> findByEarnedEpochAndType(Long epoch, InstantRewardType rewardType, Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
