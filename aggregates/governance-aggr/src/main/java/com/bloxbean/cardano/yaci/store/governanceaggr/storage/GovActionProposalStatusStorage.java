package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;

import java.util.List;
import java.util.Optional;

public interface GovActionProposalStatusStorage {
    void saveAll(List<GovActionProposalStatus> govActionProposalStatusList);
    List<GovActionProposalStatus> findByStatusAndEpoch(GovActionStatus status, int epoch);
    Optional<GovActionProposalStatus> findLastEnactedProposal(GovActionType govActionType);
}
