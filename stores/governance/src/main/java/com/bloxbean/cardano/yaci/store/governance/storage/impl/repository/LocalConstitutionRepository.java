package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalConstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalConstitutionRepository extends JpaRepository<LocalConstitutionEntity, Integer> {
    Optional<LocalConstitutionEntity> findFirstByOrderBySlotDesc();
    int deleteBySlotGreaterThan(long slot);
}
