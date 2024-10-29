package com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardCalcJobRepository extends JpaRepository<RewardCalcJobEntity, Long> {
    List<RewardCalcJobEntity> findByStatusOrderByEpoch(RewardCalcStatus status);

    int deleteBySlotGreaterThan(Long slot);
}
