package com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardRestRepository extends JpaRepository<RewardRestEntity, RewardRestId> {

}
