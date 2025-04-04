package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;

import java.util.List;

public interface GovActionProposalStorage {
    void saveAll(List<GovActionProposal> govActionProposals);

    int deleteBySlotGreaterThan(long slot);

    List<GovActionProposal> findByGovActionIds(List<GovActionId> govActionIds);
    List<GovActionProposal> findByEpoch(int epoch);
}
