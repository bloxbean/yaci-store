package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovActionProposalStatusRepository extends JpaRepository<GovActionProposalStatusEntity,
        GovActionProposalStatusId> {
    List<GovActionProposalStatusEntity> findByStatusAndEpochLessThan(GovActionStatus status, int epoch);
    int deleteBySlotGreaterThan(long slot);
}
