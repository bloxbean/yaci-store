package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.UnclaimedRewardRestEntity;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnclaimedRewardRestRepository extends JpaRepository<UnclaimedRewardRestEntity, UUID> {

    List<UnclaimedRewardRestEntity> findBySpendableEpoch(Integer spendableEpoch);

    Slice<UnclaimedRewardRestEntity> findBySpendableEpoch(Integer spendableEpoch, Pageable pageable);

    Slice<UnclaimedRewardRestEntity> findByEarnedEpoch(Integer earnedEpoch, Pageable pageable);

    int deleteByEarnedEpochAndType(Integer earnedEpoch, RewardRestType type);

    int deleteBySlotGreaterThan(Long slot);
}
