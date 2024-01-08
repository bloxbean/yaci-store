package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingProcedureRepository extends JpaRepository<VotingProcedureEntity, VotingProcedureId> {
    int deleteBySlotGreaterThan(Long slot);

    List<VotingProcedureEntity> findByTxHash(String txHash);

    Slice<VotingProcedureEntity> findByGovActionTxHash(String txHash, Pageable pageable);

    List<VotingProcedureEntity> findByGovActionTxHashAndIndex(String govActionTxHash, int govActionIndex);
}
