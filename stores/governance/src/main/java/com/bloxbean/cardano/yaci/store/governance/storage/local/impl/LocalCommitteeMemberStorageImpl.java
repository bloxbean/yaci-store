package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalCommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalCommitteeMemberRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalCommitteeMemberStorageImpl implements LocalCommitteeMemberStorage {
    private final LocalCommitteeMemberRepository localCommitteeMemberRepository;
    private final LocalCommitteeMemberMapper localCommitteeMemberMapper;

    @Override
    public void saveAll(List<LocalCommitteeMember> localCommitteeMembers) {
        localCommitteeMemberRepository.saveAll(localCommitteeMembers.stream()
                .map(localCommitteeMemberMapper::toLocalCommitteeMemberEntity).toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localCommitteeMemberRepository.deleteBySlotGreaterThan(slot);
    }
}
