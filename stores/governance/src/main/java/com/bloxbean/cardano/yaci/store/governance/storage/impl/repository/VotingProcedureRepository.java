package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotingProcedureRepository extends JpaRepository<VotingProcedureEntity, VotingProcedureId> {
    Optional<VotingProcedureEntity> findById(UUID id);

    int deleteBySlotGreaterThan(Long slot);

    List<VotingProcedureEntity> findByTxHash(String txHash);

    Slice<VotingProcedureEntity> findByGovActionTxHash(String txHash, Pageable pageable);

    Slice<VotingProcedureEntity> findByGovActionTxHashAndIndex(String govActionTxHash, long govActionIndex, Pageable pageable);

    Slice<VotingProcedureEntity> findBySlotGreaterThan(Long slot, Pageable pageable);

    List<VotingProcedureEntity> findByVoterTypeAndEpochIsGreaterThanEqual(VoterType voterType, int epoch);

    List<VotingProcedureEntity> findByVoterTypeInAndEpochEquals(List<VoterType> voterTypes, int epoch);
}
