package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;

import java.util.List;

public interface GovActionProposalStorageReader {
    List<GovActionProposal> findByTxHash(String txHash);

    List<GovActionProposal> findByGovActionType(GovActionType govActionType, int page, int count);

    List<GovActionProposal> findByReturnAddress(String address, int page, int count);
}
