package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.InstantRewardEntity;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.UUID;

@Repository
public interface InstantRewardRepository extends JpaRepository<InstantRewardEntity, UUID> {

    Slice<InstantRewardEntity> findByEarnedEpoch(Integer epoch, Pageable pageable);

    Slice<InstantRewardEntity> findByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType, Pageable pageable);

    @Query("SELECT SUM(m.amount) FROM InstantRewardEntity m WHERE m.earnedEpoch=:epoch and m.type=:type")
    BigInteger findTotalAmountByEarnedEpoch(Integer epoch, InstantRewardType type);

    int deleteBySlotGreaterThan(Long slot);
}
