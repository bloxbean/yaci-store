package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.UnclaimedRewardRestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnclaimedRewardRestRepository extends JpaRepository<UnclaimedRewardRestEntity, UUID> {

    List<UnclaimedRewardRestEntity> findBySpendableEpoch(Integer spendableEpoch);

    int deleteBySlotGreaterThan(Long slot);
}
