package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.model.BFEpochSumProjection;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "store.adapot", name = "enabled", havingValue = "true", matchIfMissing = false)
public interface BFEpochStakeRepository extends JpaRepository<EpochStakeEntity, EpochStakeId> {

    @Query("select e.activeEpoch as epoch, sum(e.amount) as total from EpochStakeEntity e where e.activeEpoch in :epochs group by e.activeEpoch")
    List<BFEpochSumProjection> getActiveStakesByEpochs(@Param("epochs") List<Integer> epochs);
}
