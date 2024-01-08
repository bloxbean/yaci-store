package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VotingProcedureService {
    private final VotingProcedureStorageReader votingProcedureStorageReader;

    public List<VotingProcedure> getVotingProcedureByTx(String txHash) {
        return votingProcedureStorageReader.findByTxHash(txHash);
    }

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTx(String txHash, int page, int count) {
        return votingProcedureStorageReader.findByGovActionTxHash(txHash, page, count);
    }

    public List<VotingProcedure> getVotingProcedureByGovActionProposalTxAndGovActionProposalIndex(String txHash, int index) {
        return votingProcedureStorageReader.findByGovActionTxHashAndGovActionIndex(txHash, index);
    }
}