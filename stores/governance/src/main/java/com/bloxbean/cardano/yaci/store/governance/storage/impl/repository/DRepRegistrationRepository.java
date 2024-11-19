package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DRepRegistrationRepository extends JpaRepository<DRepRegistrationEntity, DRepRegistrationId> {

    @Query("select dr from DRepRegistrationEntity dr where dr.type = 'REG_DREP_CERT'")
    Slice<DRepRegistrationEntity> findRegistrations(Pageable pageable);

    @Query("select dr from DRepRegistrationEntity dr where dr.type = 'UNREG_DREP_CERT'")
    Slice<DRepRegistrationEntity> findDeRegistrations(Pageable pageable);

    @Query("select dr from DRepRegistrationEntity dr where dr.type = 'UPDATE_DREP_CERT'")
    Slice<DRepRegistrationEntity> findUpdates(Pageable pageable);

    int deleteBySlotGreaterThan(long slot);

    @Query("select dr from DRepRegistrationEntity  dr where dr.slot =(select max(slot) from DRepRegistrationEntity where drepHash = dr.drepHash)")
    Page<DRepRegistrationEntity> findAllDreps(Pageable pageable);
}
