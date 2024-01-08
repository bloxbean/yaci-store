package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovActionProposalRepository extends JpaRepository<GovActionProposalEntity, GovActionProposalId> {
    int deleteBySlotGreaterThan(Long slot);

    List<GovActionProposalEntity> findByTxHash(String txHash);

    Slice<GovActionProposalEntity> findByType(GovActionType type, Pageable pageable);

    Slice<GovActionProposalEntity> findByReturnAddress(String address, Pageable pageable);
}
