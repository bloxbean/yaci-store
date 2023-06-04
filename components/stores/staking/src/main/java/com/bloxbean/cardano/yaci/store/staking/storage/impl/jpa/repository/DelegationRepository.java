package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationRepository
        extends JpaRepository<DelegationEntity, DelegationId> {

    @Query("select d from DelegationEntity d")
    Slice<DelegationEntity> findDelegations(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
