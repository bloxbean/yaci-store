package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntityJpa;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GovActionProposalRepository extends JpaRepository<GovActionProposalEntityJpa, GovActionProposalId> {
    int deleteBySlotGreaterThan(Long slot);

    List<GovActionProposalEntityJpa> findByTxHash(String txHash);

    Slice<GovActionProposalEntityJpa> findByType(GovActionType type, Pageable pageable);

    Slice<GovActionProposalEntityJpa> findByReturnAddress(String address, Pageable pageable);

    @Query("select ga from GovActionProposalEntityJpa ga where ga.type = :type order by ga.slot desc limit 1")
    Optional<GovActionProposalEntityJpa> findMostRecentGovActionByType(GovActionType type);
}
