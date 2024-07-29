package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitteeRepository extends JpaRepository<CommitteeEntity, Integer> {
    Optional<CommitteeEntity> findFirstByOrderByEpochDesc();
    int deleteBySlotGreaterThan(long slot);
}
