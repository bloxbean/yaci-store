package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.GovUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VotingProcedureService {
    private final VotingProcedureStorageReader votingProcedureStorageReader;

    public Optional<VotingProcedure> getVotingProcedureById(UUID id) {
        return votingProcedureStorageReader.findById(id);
    }

    public List<VotingProcedure> getVotingProcedureByTx(String txHash) {
        return votingProcedureStorageReader.findByTxHash(txHash);
    }

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTx(String txHash, int page, int count, Order order) {
        return votingProcedureStorageReader.findByGovActionTxHash(txHash, page, count, order);
    }

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTxAndGovActionProposalIndex(String txHash, int index,
                                                                                                  int page, int count, Order order) {
        return votingProcedureStorageReader.findByGovActionTxHashAndGovActionIndex(txHash, index, page, count, order);
    }

    public List<VotingProcedure> getVotingProcedureList(int page, int count, Order order) {
        return votingProcedureStorageReader.findAll(page, count, order);
    }

    /**
     * Get voting procedures for a governance action by CIP-129 bech32 governance action ID
     * @param govActionIdBech32 The governance action ID in bech32 format (e.g., gov_action1...)
     * @param page Page number
     * @param count Number of items per page
     * @param order Sort order
     * @return List of voting procedures for the governance action
     */
    public List<VotingProcedure> getVotingProcedureByGovActionId(String govActionIdBech32, int page, int count, Order order) {
        var govActionId = GovUtil.toGovActionIdFromBech32(govActionIdBech32);
        return votingProcedureStorageReader.findByGovActionTxHashAndGovActionIndex(
                govActionId.getTransactionId(),
                govActionId.getGovActionIndex(),
                page,
                count,
                order
        );
    }
}
