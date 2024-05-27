package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeMemberRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeMemberStorageReaderImpl implements CommitteeMemberStorageReader {
    private final CommitteeMemberRepository committeeMemberRepository;
    private final CommitteeMemberMapper committeeMemberMapper;

    @Override
    public List<CommitteeMember> findCurrentMembers() {
        return committeeMemberRepository.findCommitteeMemberEntitiesWithMaxSlot().stream()
                .map(committeeMemberMapper::toCommitteeMember)
                .collect(Collectors.toList());
    }

}
