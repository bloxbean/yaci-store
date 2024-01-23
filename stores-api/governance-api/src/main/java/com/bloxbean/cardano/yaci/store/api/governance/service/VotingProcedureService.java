package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.common.model.Order;
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

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTx(String txHash, int page, int count) {
        return votingProcedureStorageReader.findByGovActionTxHash(txHash, page, count);
    }

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTxAndGovActionProposalIndex(String txHash, int index) {
        return votingProcedureStorageReader.findByGovActionTxHashAndGovActionIndex(txHash, index);
    }

    public List<VotingProcedure> getVotingProcedureList(int page, int count, Order order) {
        return votingProcedureStorageReader.findAll(page, count, order);
    }
}
