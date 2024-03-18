package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotingProcedureRepository extends JpaRepository<VotingProcedureEntityJpa, VotingProcedureId> {
    Optional<VotingProcedureEntityJpa> findById(UUID id);

    int deleteBySlotGreaterThan(Long slot);

    List<VotingProcedureEntityJpa> findByTxHash(String txHash);

    Slice<VotingProcedureEntityJpa> findByGovActionTxHash(String txHash, Pageable pageable);

    Slice<VotingProcedureEntityJpa> findByGovActionTxHashAndIndex(String govActionTxHash, long govActionIndex, Pageable pageable);
}
