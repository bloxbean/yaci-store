package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlockStorageImpl implements BlockStorage {
    private final BlockRepository blockRepository;
    private final BlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return blockRepository.findTopByOrderByNumberDesc()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return blockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity))
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return blockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void save(Block block) {
        BlockEntity blockEntity = blockDetailsMapper.toBlockEntity(block);
        blockRepository.save(blockEntity);
    }
}
