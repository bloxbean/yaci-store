package com.bloxbean.cardano.yaci.store.api.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.api.epoch.storage.ProtocolParamsProposalReader;
import com.bloxbean.cardano.yaci.store.api.epoch.storage.impl.repository.ProtocolParamsProposalReadRepository;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.ProtocolParamsProposalEntity;
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
public class ProtocolParamsProposalReaderImpl implements ProtocolParamsProposalReader {
    private final ProtocolParamsProposalReadRepository protocolParamsProposalReadRepository;
    private final ProtocolParamsMapper mapper;

    @Override
    public List<ProtocolParamsProposal> getProtocolParamsProposals(int page, int count, Order order) {
        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        Page<ProtocolParamsProposalEntity> entityPageable = protocolParamsProposalReadRepository.findAll(pageable);

        List<ProtocolParamsProposalEntity> entities = entityPageable.getContent();
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
