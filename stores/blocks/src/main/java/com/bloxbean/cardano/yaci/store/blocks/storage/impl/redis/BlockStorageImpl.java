package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public BlocksPage findBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("number").descending());

        Page<BlockEntity> blocksEntityPage = blockRepository.findAll(sortedByBlock);
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
        return blockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockDetailsMapper::toBlock)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> findByBlockHash(String blockHash) {
        return blockRepository.findByHash(blockHash)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public Optional<Block> findByBlock(long block) {
        return blockRepository.findByNumber(block)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public List<PoolBlock> findBlocksBySlotLeaderAndEpoch(String slotLeader, int epoch) {
        return blockRepository.getBlockEntitiesBySlotLeaderAndEpochNumber(slotLeader, epoch)
                .stream()
                .map(blockEntity -> {
                    return PoolBlock.builder()
                            .hash(blockEntity.getHash())
                            .number(blockEntity.getNumber())
                            .epoch(blockEntity.getEpochNumber())
                            .poolId(blockEntity.getSlotLeader())
                            .build();
                }).collect(Collectors.toList());
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
