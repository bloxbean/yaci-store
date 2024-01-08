package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.GovActionProposalMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GovActionProposalStorageImpl implements GovActionProposalStorage {

    private final GovActionProposalRepository govActionProposalRepository;
    private final GovActionProposalMapper govActionProposalMapper;

    @Override
    public void saveAll(List<GovActionProposal> govActionProposals) {
        govActionProposalRepository.saveAll(govActionProposals.stream()
                .map(govActionProposalMapper::toGovActionProposalEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return govActionProposalRepository.deleteBySlotGreaterThan(slot);
    }
}
