package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.persistence.EpochPersistence;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.EpochEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpochJpaPersistence  implements EpochPersistence {

    private final EpochJpaRepository epochJpaRepository;
    private final EpochMapper epochMapper;

    @Override
    public Optional<Epoch> findRecentEpoch() {
        return epochJpaRepository.findTopByOrderByNumberDesc()
                .map(epochEntity -> epochMapper.toEpoch(epochEntity));
    }

    @Override
    public void save(Epoch epoch) {
        EpochEntity epochEntity = epochMapper.toEpochEntity(epoch);
        epochJpaRepository.save(epochEntity);
    }

    @Override
    public Optional<Epoch> findByNumber(int number) {
        return epochJpaRepository.findByNumber(number);
    }

    @Override
    public EpochsPage findEpochs(int page, int count) {
        Pageable sortedByEpoch =
                PageRequest.of(page, count, Sort.by("number").descending());

        Page<EpochEntity> epochsEntityPage = epochJpaRepository.findAll(sortedByEpoch);
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
}
