package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationRepository
        extends JpaRepository<DelegationEntityJpa, DelegationId> {

    @Query("select d from DelegationEntityJpa d")
    Slice<DelegationEntityJpa> findDelegations(Pageable pageable);

    int deleteBySlotGreaterThan(Long slot);
}
