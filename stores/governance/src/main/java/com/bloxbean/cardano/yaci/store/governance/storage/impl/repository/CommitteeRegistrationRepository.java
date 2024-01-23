package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitteeRegistrationRepository extends JpaRepository<CommitteeRegistrationEntity, CommitteeRegistrationId> {
    int deleteBySlotGreaterThan(long slot);
}
