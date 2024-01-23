package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DelegationVoteMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DelegationVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DelegationVoteStorageReaderImpl implements DelegationVoteStorageReader {
    private final DelegationVoteRepository delegationVoteRepository;
    private final DelegationVoteMapper delegationVoteMapper;

    @Override
    public List<DelegationVote> findAll(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return delegationVoteRepository.findAll(sortedBySlot)
                .stream()
                .map(delegationVoteMapper::toDelegationVote)
                .collect(Collectors.toList());
    }

    @Override
    public List<DelegationVote> findByDRepId(String dRepId, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return delegationVoteRepository.findByDrepId(dRepId, sortedBySlot).stream().map(delegationVoteMapper::toDelegationVote)
                .collect(Collectors.toList());
    }

    @Override
    public List<DelegationVote> findByAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return delegationVoteRepository.findByAddress(address, sortedBySlot).stream().map(delegationVoteMapper::toDelegationVote)
                .collect(Collectors.toList());
    }
}
