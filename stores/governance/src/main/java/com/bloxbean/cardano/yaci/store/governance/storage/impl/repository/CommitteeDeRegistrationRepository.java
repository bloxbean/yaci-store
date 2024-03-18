package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeDeRegistrationEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeDeRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitteeDeRegistrationRepository extends JpaRepository<CommitteeDeRegistrationEntityJpa, CommitteeDeRegistrationId> {
    int deleteBySlotGreaterThan(Long slot);
}
