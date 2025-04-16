package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.GovActionProposalMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class GovActionProposalStorageReaderImpl implements GovActionProposalStorageReader {
    private final GovActionProposalRepository govActionProposalRepository;
    private final GovActionProposalMapper govActionProposalMapper;

    @Override
    public List<GovActionProposal> findAll(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findAll(sortedBySlot);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByTxHash(String txHash) {
        List<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByTxHash(txHash);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByGovActionType(GovActionType govActionType, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByType(govActionType, sortedBySlot);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByReturnAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        Slice<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByReturnAddress(address, sortedBySlot);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public Optional<GovActionProposal> findMostRecentGovActionByType(GovActionType govActionType) {
        return govActionProposalRepository.findMostRecentGovActionByType(govActionType).map(govActionProposalMapper::toGovActionProposal);
    }

    @Override
    public Optional<GovActionProposal> findByGovActionTxHashAndGovActionIndex(String txHash, int index) {
        return govActionProposalRepository.findByTxHashAndIndex(txHash, index).map(govActionProposalMapper::toGovActionProposal);
    }
}
