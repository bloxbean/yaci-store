package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitteeRegistrationRepository extends JpaRepository<CommitteeRegistrationEntityJpa, CommitteeRegistrationId> {
    int deleteBySlotGreaterThan(long slot);
}
