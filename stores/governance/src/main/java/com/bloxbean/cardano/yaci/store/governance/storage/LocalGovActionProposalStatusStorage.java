package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalGovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;

import java.util.List;

public interface LocalGovActionProposalStatusStorage {
    void saveAll(List<LocalGovActionProposalStatus> localGovActionProposalStatusList);
    List<LocalGovActionProposalStatus> findByEpochAndStatusIn(Integer epochNo, List<GovActionStatus> statusList);
    int deleteBySlotGreaterThan(long slot);
}
