package com.bloxbean.cardano.yaci.store.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorageReader;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.ProtocolParamsProposalEntityJpa;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.ProtocolParamsProposalRepository;
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
public class ProtocolParamsProposalStorageReaderImpl implements ProtocolParamsProposalStorageReader {
    private final ProtocolParamsProposalRepository protocolParamsProposalReadRepository;
    private final ProtocolParamsMapper mapper;

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposals(int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        Page<ProtocolParamsProposalEntityJpa> entityPageable = protocolParamsProposalReadRepository.findAll(pageable);

        List<ProtocolParamsProposalEntityJpa> entities = entityPageable.getContent();
        if (entities == null || entities.isEmpty())
            return Collections.EMPTY_LIST;

        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposalsByCreateEpoch(int epoch) {
        return protocolParamsProposalReadRepository.findByEpoch(epoch)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
