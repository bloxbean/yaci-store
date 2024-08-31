package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalCommitteeMemberRepository extends JpaRepository<LocalCommitteeMemberEntity, Integer> {
    int deleteBySlotGreaterThan(long slot);
}
