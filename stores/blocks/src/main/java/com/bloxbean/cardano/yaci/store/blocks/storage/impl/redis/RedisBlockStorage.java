package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RedisBlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.RedisBlockRepository;
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
    public Integer deleteBySlotGreaterThan(long slot) {
        return redisBlockRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void save(Block block) {
        RedisBlockEntity redisBlockEntity = blockDetailsMapper.toBlockEntity(block);
        redisBlockRepository.save(redisBlockEntity);
    }
}