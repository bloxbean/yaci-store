package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalHardForkInitiationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalHardForkInitiationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalHardForkInitiationRepository extends JpaRepository<LocalHardForkInitiationEntity, LocalHardForkInitiationId> {
    int deleteBySlotGreaterThan(long slot);
}
