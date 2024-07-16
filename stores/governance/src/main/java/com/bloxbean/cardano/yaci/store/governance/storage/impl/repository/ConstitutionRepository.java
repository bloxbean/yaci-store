package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.ConstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConstitutionRepository extends JpaRepository<ConstitutionEntity, Integer> {
    Optional<ConstitutionEntity> findFirstByOrderBySlotDesc();

    int deleteBySlotGreaterThan(long slot);
}
