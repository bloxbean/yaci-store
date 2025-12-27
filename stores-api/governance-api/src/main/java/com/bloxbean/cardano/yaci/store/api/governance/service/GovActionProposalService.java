package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
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

    public List<GovActionProposal> getGovActionProposalByGovActionType(GovActionType type, int page, int count, Order order) {
        return govActionProposalStorageReader.findByGovActionType(type, page, count, order);
    }

    public List<GovActionProposal> getGovActionProposalByReturnAddress(String address, int page, int count, Order order) {
        return govActionProposalStorageReader.findByReturnAddress(address, page, count, order);
    }

    public List<GovActionProposal> getGovActionProposalList(int page, int count, Order order) {
        return govActionProposalStorageReader.findAll(page, count, order);
    }

    public Optional<GovActionProposal> getMostRecentGovActionProposalByGovActionType(GovActionType type) {
        return govActionProposalStorageReader.findMostRecentGovActionByType(type);
    }

    /**
     * Get governance action proposal by CIP-129 bech32 governance action ID
     * @param govActionIdBech32 The governance action ID in bech32 format (e.g., gov_action1...)
     * @return Optional containing the governance action proposal if found
     */
    public Optional<GovActionProposal> getGovActionProposalByGovActionId(String govActionIdBech32) {
        var govActionId = GovUtil.toGovActionIdFromBech32(govActionIdBech32);
        return govActionProposalStorageReader.findByGovActionTxHashAndGovActionIndex(
                govActionId.getTransactionId(),
                govActionId.getGovActionIndex()
        );
    }
}
