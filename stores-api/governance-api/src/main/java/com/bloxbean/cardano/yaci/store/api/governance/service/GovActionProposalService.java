package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public List<GovActionProposal> getGovActionProposalList(int page, int count, Order order) {
        return govActionProposalStorageReader.findAll(page, count, order);
    }

    public Optional<GovActionProposal> getMostRecentGovActionProposalByGovActionType(GovActionType type) {
        return govActionProposalStorageReader.findMostRecentGovActionByType(type);
    }
}
