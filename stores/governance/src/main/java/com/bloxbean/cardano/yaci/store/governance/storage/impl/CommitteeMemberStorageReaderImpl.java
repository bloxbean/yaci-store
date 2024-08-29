package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeMemberStorageReaderImpl implements CommitteeMemberStorageReader {
    private final CommitteeMemberRepository committeeMemberRepository;
    private final CommitteeMemberMapper committeeMemberMapper;

    @Override
    public List<CommitteeMember> findAll(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return committeeMemberRepository.findAll(sortedBySlot)
                .stream()
                .map(committeeMemberMapper::toCommitteeMember)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommitteeMember> findCommitteeMembersWithMaxSlot() {
        return committeeMemberRepository.findCommitteeMemberEntitiesWithMaxSlot().stream()
                .map(committeeMemberMapper::toCommitteeMember).toList();
    }
}
