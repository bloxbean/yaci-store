package com.bloxbean.cardano.yaci.store.api.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.api.blocks.storage.EpochReader;
import com.bloxbean.cardano.yaci.store.api.blocks.storage.impl.repository.EpochReadRepository;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.EpochEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EpochReaderImpl implements EpochReader {
    private final EpochReadRepository epochRepository;
    private final EpochMapper epochMapper;

    @Override
    public Optional<Epoch> findRecentEpoch() {
        return epochRepository.findTopByOrderByNumberDesc()
                .map(epochEntity -> epochMapper.toEpoch(epochEntity));
    }

    @Override
    public EpochsPage findEpochs(int page, int count) {
        Pageable sortedByEpoch =
                PageRequest.of(page, count, Sort.by("number").descending());

        Page<EpochEntity> epochsEntityPage = epochRepository.findAll(sortedByEpoch);
        long total = epochsEntityPage.getTotalElements();
        int totalPage = epochsEntityPage.getTotalPages();

        List<Epoch> epochList = epochsEntityPage.stream()
                .map(epochEntity -> epochMapper.toEpoch(epochEntity))
                .collect(Collectors.toList());

        return EpochsPage.builder()
                .total(total)
                .totalPages(totalPage)
                .epochs(epochList)
                .build();
    }

    @Override
    public Optional<Epoch> findByNumber(int number) {
        return epochRepository.findById((long)number)
                .map(epochEntity -> epochMapper.toEpoch(epochEntity));
    }
}
