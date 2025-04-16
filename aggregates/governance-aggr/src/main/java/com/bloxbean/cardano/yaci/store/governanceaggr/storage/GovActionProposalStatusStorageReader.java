package com.bloxbean.cardano.yaci.store.governanceaggr.storage;


import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;

import java.util.List;

public interface GovActionProposalStatusStorageReader {
    List<GovActionProposalStatus> findLatestStatusesForProposals(List<GovActionId> govActionIds);
}
