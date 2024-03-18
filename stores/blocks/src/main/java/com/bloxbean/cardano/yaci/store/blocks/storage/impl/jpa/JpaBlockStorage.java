package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.JpaBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaBlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaBlockRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JpaBlockStorage implements BlockStorage {

    private final JpaBlockRepository jpaBlockRepository;
    private final JpaBlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return jpaBlockRepository.findTopByOrderByNumberDesc()
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return jpaBlockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockDetailsMapper::toBlock)
                .collect(Collectors.toList());
    }

    @Override
    public Integer deleteBySlotGreaterThan(long slot) {
        return jpaBlockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void save(Block block) {
        JpaBlockEntity jpaBlockEntity = blockDetailsMapper.toBlockEntity(block);
        jpaBlockRepository.save(jpaBlockEntity);
    }
}
