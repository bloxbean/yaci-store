package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DelegationVoteMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DelegationVoteRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DelegationVoteStorageImpl implements DelegationVoteStorage {
    private final DelegationVoteRepository delegationVoteRepository;
    private final DelegationVoteMapper delegationVoteMapper;

    @Override
    public void saveAll(List<DelegationVote> delegationVotes) {
        delegationVoteRepository.saveAll(delegationVotes.stream()
                .map(delegationVoteMapper::toDelegationVoteEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return delegationVoteRepository.deleteBySlotGreaterThan(slot);
    }
}
