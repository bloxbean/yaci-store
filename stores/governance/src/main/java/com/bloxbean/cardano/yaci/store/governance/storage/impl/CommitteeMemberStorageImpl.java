package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeMemberRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeMemberStorageImpl implements CommitteeMemberStorage {
    private final CommitteeMemberRepository committeeMemberRepository;
    private final CommitteeMemberMapper committeeMemberMapper;

    @Override
    public void saveAll(List<CommitteeMember> committeeMembers) {
        committeeMemberRepository.saveAll(committeeMembers.stream()
                .map(committeeMemberMapper::toCommitteeMemberEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public List<CommitteeMember> getCommitteeMembersByEpoch(int epoch) {
        return committeeMemberRepository.findCommitteeMembersByEpoch(epoch)
                .stream()
                .map(committeeMemberMapper::toCommitteeMember)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeMemberRepository.deleteBySlotGreaterThan(slot);
    }
}
