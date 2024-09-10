package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalDRepDistrRepository extends JpaRepository<LocalDRepDistrEntity, LocalDRepDistrId> {
    Optional<LocalDRepDistrEntity> findByDrepHashAndEpoch(String dRepHash, Integer epoch);
    int deleteBySlotGreaterThan(long slot);
}
