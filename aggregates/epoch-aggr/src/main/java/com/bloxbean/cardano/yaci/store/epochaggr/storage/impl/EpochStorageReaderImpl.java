package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorageReader;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.JpaEpochEntity;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.repository.EpochRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EpochStorageReaderImpl implements EpochStorageReader {
    private final EpochRepository epochRepository;
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

        Page<JpaEpochEntity> epochsEntityPage = epochRepository.findAll(sortedByEpoch);
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
