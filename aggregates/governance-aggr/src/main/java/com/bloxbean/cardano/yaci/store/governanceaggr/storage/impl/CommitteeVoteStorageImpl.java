package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.CommitteeVoteMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeVoteRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeVoteStorageImpl implements CommitteeVoteStorage {
    private final CommitteeVoteRepository committeeVoteRepository;
    private final CommitteeVoteMapper committeeVoteMapper;

    @Override
    public void saveAll(List<CommitteeVote> committeeVotes) {
        committeeVoteRepository.saveAll(committeeVotes.stream().map(committeeVoteMapper::toCommitteeVotesEntity)
                .collect(Collectors.toList()));
    }
}
