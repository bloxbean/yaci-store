package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DrepRegistrationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DrepRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrepRegistrationRepository extends JpaRepository<DrepRegistrationEntity, DrepRegistrationId> {
    int deleteBySlotGreaterThan(long slot);
}
