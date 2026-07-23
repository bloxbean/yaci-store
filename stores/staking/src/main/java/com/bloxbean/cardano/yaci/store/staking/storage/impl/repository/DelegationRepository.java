package com.bloxbean.cardano.yaci.store.staking.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DelegationRepository
        extends JpaRepository<DelegationEntity, DelegationId> {

    @Query("select d from DelegationEntity d")
    Slice<DelegationEntity> findDelegations(Pageable pageable);

    @Query("select d from DelegationEntity d where d.address = :stakeAddress order by d.slot desc, d.txIndex desc, d.certIndex desc limit 1")
    Optional<DelegationEntity> findLatestDelegationByAddress(String stakeAddress);

    int deleteBySlotGreaterThan(Long slot);
}
