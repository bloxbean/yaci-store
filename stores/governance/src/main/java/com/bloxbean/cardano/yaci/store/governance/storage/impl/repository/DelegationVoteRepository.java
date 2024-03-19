package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationVoteRepository extends JpaRepository<DelegationVoteEntity, DelegationVoteId> {
    Slice<DelegationVoteEntity> findByDrepId(String dRepId, Pageable pageable);

    Slice<DelegationVoteEntity> findByAddress(String address, Pageable pageable);

    int deleteBySlotGreaterThan(long slot);
}
