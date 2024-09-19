package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalCommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalCommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalCommitteeMemberRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalCommitteeMemberStorageReaderImpl implements LocalCommitteeMemberStorageReader {
    private final LocalCommitteeMemberRepository localCommitteeMemberRepository;
    private final LocalCommitteeMemberMapper localCommitteeMemberMapper;

    @Override
    public List<LocalCommitteeMember> findCommitteeMembersWithMaxSlot() {
        return localCommitteeMemberRepository.findLocalCommitteeMemberEntitiesWithMaxSlot()
                .stream().map(localCommitteeMemberMapper::toLocalCommitteeMember)
                .toList();
    }
}
