package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RewardRestRepository extends JpaRepository<RewardRestEntity, UUID> {

    int deleteBySlotGreaterThan(Long slot);

    List<RewardRestEntity> findBySpendableEpochAndType(Integer spendableEpoch, RewardRestType type);
}
