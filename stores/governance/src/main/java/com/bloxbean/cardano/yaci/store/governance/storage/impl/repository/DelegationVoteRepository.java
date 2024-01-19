package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DelegationVoteRepository extends JpaRepository<DelegationVoteEntity, DelegationVoteId> {
    List<DelegationVoteEntity> findByDrepId(String dRepId);

    int deleteBySlotGreaterThan(long slot);
}
