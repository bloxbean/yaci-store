package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RewardRestRepository extends JpaRepository<RewardRestEntity, UUID> {

    int deleteBySlotGreaterThan(Long slot);

    List<RewardRestEntity> findBySpendableEpochAndType(Integer spendableEpoch, RewardRestType type);

    Slice<RewardRestEntity> findByEarnedEpoch(Integer earnedEpoch, Pageable pageable);
    Slice<RewardRestEntity> findBySpendableEpoch(Integer earnedEpoch, Pageable pageable);

    List<RewardRestEntity> findByAddressAndEarnedEpoch(String address, Integer earnedEpoch);
    List<RewardRestEntity> findByAddressAndSpendableEpoch(String address, Integer spendableEpoch);
    Slice<RewardRestEntity> findByAddress(String address, Pageable pageable);

    int deleteByEarnedEpochAndType(Integer earnedEpoch, RewardRestType type);
}
