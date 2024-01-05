package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
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

@RequiredArgsConstructor
@Slf4j
public class GovActionProposalStorageReaderImpl implements GovActionProposalStorageReader {
    private final GovActionProposalRepository govActionProposalRepository;
    private final GovActionProposalMapper govActionProposalMapper;

    @Override
    public List<GovActionProposal> findByTxHash(String txHash) {
        List<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByTxHash(txHash);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByGovActionType(GovActionType govActionType, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        Slice<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByType(govActionType, sortedBySlot);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }

    @Override
    public List<GovActionProposal> findByReturnAddress(String address, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        Slice<GovActionProposalEntity> govActionProposalEntities = govActionProposalRepository.findByReturnAddress(address, sortedBySlot);
        return govActionProposalEntities.stream().map(govActionProposalMapper::toGovActionProposal).toList();
    }
}
