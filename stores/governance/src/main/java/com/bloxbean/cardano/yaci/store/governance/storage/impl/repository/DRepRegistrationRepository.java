package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DRepRegistrationRepository extends JpaRepository<DRepRegistrationEntity, DRepRegistrationId> {
    int deleteBySlotGreaterThan(long slot);
}
