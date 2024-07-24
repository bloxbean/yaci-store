package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalCommitteeRepository extends JpaRepository<LocalCommitteeEntity, Integer> {
    int deleteBySlotGreaterThan(long slot);
}
