package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;

import java.util.List;

public interface GovActionProposalStatusStorage {
    void saveAll(List<GovActionProposalStatus> govActionProposalStatusList);
    List<GovActionProposalStatus> findByStatusAndEpochLessThanEqual(GovActionStatus status, int epoch);
    int deleteBySlotGreaterThan(long slot);
}
