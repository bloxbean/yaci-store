package com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.mapper.RedisBlockMapper;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.model.RedisBlockEntity;
import com.bloxbean.cardano.yaci.store.extensions.redis.blocks.impl.repository.RedisBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RedisBlockStorageReader implements BlockStorageReader {

    private final RedisBlockRepository redisBlockRepository;
    private final RedisBlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return redisBlockRepository.findTopByOrderByNumberDesc()
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public BlocksPage findBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("number").descending());

        //TODO -- Fix once the count query is fixed
        Slice<RedisBlockEntity> blocksEntityPage = redisBlockRepository.findAll(sortedByBlock);
//      long total = blocksEntityPage.getTotalElements();
//      int totalPage = blocksEntityPage.getTotalPages();

        List<BlockSummary> blockSummaryList = blocksEntityPage.stream()
                .map(blockDetailsMapper::toBlockSummary)
                .collect(Collectors.toList());

        return BlocksPage.builder()
//                .total(total)
//                .totalPages(totalPage)
                .blocks(blockSummaryList)
                .build();
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return redisBlockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockDetailsMapper::toBlock)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> findByBlockHash(String blockHash) {
        return redisBlockRepository.findById(blockHash)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public Optional<Block> findByBlock(long block) {
        return redisBlockRepository.findByNumber(block)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public List<PoolBlock> findBlocksBySlotLeaderAndEpoch(String slotLeader, int epoch) {
        return redisBlockRepository.getBlockEntitiesBySlotLeaderAndEpochNumber(slotLeader, epoch)
                .stream()
                .map(redisBlockEntity -> PoolBlock.builder()
                        .hash(redisBlockEntity.getHash())
                        .number(redisBlockEntity.getNumber())
                        .epoch(redisBlockEntity.getEpochNumber())
                        .poolId(redisBlockEntity.getSlotLeader())
                        .build()).collect(Collectors.toList());
    }

}
