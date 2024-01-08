package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GovActionProposalService {
    private final GovActionProposalStorageReader govActionProposalStorageReader;

    public List<GovActionProposal> getGovActionProposalByTx(String txHash) {
        return govActionProposalStorageReader.findByTxHash(txHash);
    }

    public List<GovActionProposal> getGovActionProposalByGovActionType(GovActionType type, int page, int count) {
        return govActionProposalStorageReader.findByGovActionType(type, page, count);
    }

    public List<GovActionProposal> getGovActionProposalByReturnAddress(String address, int page, int count) {
        return govActionProposalStorageReader.findByReturnAddress(address, page, count);
    }
}
