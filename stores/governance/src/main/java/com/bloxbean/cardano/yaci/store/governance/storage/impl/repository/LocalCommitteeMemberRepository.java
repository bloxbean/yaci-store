package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalCommitteeMemberRepository extends JpaRepository<LocalCommitteeMemberEntity, Integer> {
    int deleteBySlotGreaterThan(long slot);

    @Query("select lc from LocalCommitteeMemberEntity lc where lc.slot = (select max(lc.slot) from LocalCommitteeMemberEntity lc)")
    List<LocalCommitteeMemberEntity> findLocalCommitteeMemberEntitiesWithMaxSlot();
}
