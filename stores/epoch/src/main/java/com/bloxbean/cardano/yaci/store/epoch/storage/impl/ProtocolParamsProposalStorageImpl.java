package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.ProtocolParamsProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ProtocolParamsProposalStorageImpl implements ProtocolParamsProposalStorage {
    private final ProtocolParamsProposalRepository protocolParamsProposalRepository;
    private final ProtocolParamsMapper mapper;

    @Override
    public void saveAll(List<ProtocolParamsProposal> protocolParamsProposals) {
        if (protocolParamsProposals == null || protocolParamsProposals.isEmpty())
            return;

        var entities = protocolParamsProposals.stream()
                .map(mapper::toEntity).toList();

        protocolParamsProposalRepository.saveAll(entities);
    }

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposalsByTargetEpoch(int epoch) {
        return protocolParamsProposalRepository.findByTargetEpoch(epoch)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return protocolParamsProposalRepository.deleteBySlotGreaterThan(slot);
    }
}
