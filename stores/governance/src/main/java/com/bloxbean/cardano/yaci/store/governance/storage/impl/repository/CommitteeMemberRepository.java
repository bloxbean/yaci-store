package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeMemberEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitteeMemberRepository extends JpaRepository<CommitteeMemberEntity, CommitteeMemberId> {
    int deleteBySlotGreaterThan(long slot);

    @Query("select c from CommitteeMemberEntity c where c.slot = (select max(c.slot) from CommitteeMemberEntity c)")
    List<CommitteeMemberEntity> findCommitteeMemberEntitiesWithMaxSlot();
}
