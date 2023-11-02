package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.ProtocolParamsProposalEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.ProtocolParamsProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
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
    public List<ProtocolParamsProposal> getProtocolParamsProposals(int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        Page<ProtocolParamsProposalEntity> entityPageable = protocolParamsProposalRepository.findAll(pageable);

        List<ProtocolParamsProposalEntity> entities = entityPageable.getContent();
        if (entities == null || entities.isEmpty())
            return Collections.EMPTY_LIST;

        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposalsByTargetEpoch(int epoch) {
        return protocolParamsProposalRepository.findByTargetEpoch(epoch)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposalsByCreateEpoch(int epoch) {
        return protocolParamsProposalRepository.findByEpoch(epoch)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return protocolParamsProposalRepository.deleteBySlotGreaterThan(slot);
    }
}
