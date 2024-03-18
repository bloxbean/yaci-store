package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DRepRegistrationRepository extends JpaRepository<DRepRegistrationEntityJpa, DRepRegistrationId> {

    @Query("select dr from DRepRegistrationEntityJpa dr where dr.type = 'REG_DREP_CERT'")
    Slice<DRepRegistrationEntityJpa> findRegistrations(Pageable pageable);

    @Query("select dr from DRepRegistrationEntityJpa dr where dr.type = 'UNREG_DREP_CERT'")
    Slice<DRepRegistrationEntityJpa> findDeRegistrations(Pageable pageable);

    @Query("select dr from DRepRegistrationEntityJpa dr where dr.type = 'UPDATE_DREP_CERT'")
    Slice<DRepRegistrationEntityJpa> findUpdates(Pageable pageable);

    int deleteBySlotGreaterThan(long slot);
}
