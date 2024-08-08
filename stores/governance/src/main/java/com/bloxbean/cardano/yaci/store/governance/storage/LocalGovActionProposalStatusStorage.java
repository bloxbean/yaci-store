package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalGovActionProposalStatus;

import java.util.List;

public interface LocalGovActionProposalStatusStorage {
    void saveAll(List<LocalGovActionProposalStatus> localGovActionProposalStatusList);

    int deleteBySlotGreaterThan(long slot);
}
