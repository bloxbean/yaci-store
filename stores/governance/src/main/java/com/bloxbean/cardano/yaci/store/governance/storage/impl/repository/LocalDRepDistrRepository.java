package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalDRepDistrRepository extends JpaRepository<LocalDRepDistrEntity, LocalDRepDistrId> {
    Optional<LocalDRepDistrEntity> findFirstByDrepHashOrderByEpochDesc(String dRepHash);
    int deleteBySlotGreaterThan(long slot);
}
