package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalConstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalConstitutionRepository extends JpaRepository<LocalConstitutionEntity, Integer> {
    int deleteBySlotGreaterThan(long slot);
}
