package com.bloxbean.cardano.yaci.store.blocks.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.repository.BlockRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlockStorageImpl implements BlockStorage {
    private final static String PLUGIN_BLOCK_SAVE = "block.save";
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
    @Transactional
    public int deleteBySlotGreaterThan(long slot) {
        return blockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    @Plugin(key = PLUGIN_BLOCK_SAVE)
    @Transactional
    public void save(Block block) {
        BlockEntity blockEntity = blockDetailsMapper.toBlockEntity(block);
        blockRepository.save(blockEntity);
    }
}
