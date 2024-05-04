package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.CommitteeVoteMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeVoteRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CommitteeVoteStorageReaderImpl implements CommitteeVoteStorageReader {
    private final CommitteeVoteRepository committeeVoteRepository;
    private final CommitteeVoteMapper committeeVoteMapper;

    @Override
    public List<CommitteeVote> findByGovActionTxHashAndGovActionIndexPairsWithMaxSlot(List<GovActionId> govActionIds) {
        return committeeVoteRepository.findByGovActionTxHashAndGovActionIndexPairsWithMaxSlot(govActionIds)
                .stream().map(committeeVoteMapper::toCommitteeVotes).toList();
    }
}
