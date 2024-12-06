package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitteeRepository extends JpaRepository<CommitteeEntity, Integer> {
    Optional<CommitteeEntity> findFirstByOrderByEpochDesc();

    @Query("select c from CommitteeEntity c where c.epoch <= :epoch order by c.epoch desc limit 1")
    Optional<CommitteeEntity> findCommitteeByEpoch(Integer epoch);
    int deleteBySlotGreaterThan(long slot);
}
