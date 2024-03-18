package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationVoteRepository extends JpaRepository<DelegationVoteEntityJpa, DelegationVoteId> {
    Slice<DelegationVoteEntityJpa> findByDrepId(String dRepId, Pageable pageable);

    Slice<DelegationVoteEntityJpa> findByAddress(String address, Pageable pageable);

    int deleteBySlotGreaterThan(long slot);
}
