package com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.model.RedisBlockEntity;
import com.bloxbean.cardano.yaci.store.extensions.blocks.redis.storage.impl.repository.RedisBlockRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedisBlockStorage implements BlockStorage {

    private final RedisBlockRepository redisBlockRepository;
    private final RedisBlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return redisBlockRepository.findTopByOrderByNumberDesc()
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return redisBlockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockDetailsMapper::toBlock)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return redisBlockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void save(Block block) {
        RedisBlockEntity redisBlockEntity = blockDetailsMapper.toBlockEntity(block);
        redisBlockRepository.save(redisBlockEntity);
    }
}