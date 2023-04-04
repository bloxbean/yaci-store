package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.BlockEntity;
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
public class BlockJpaPersistence implements BlockPersistence {
    private final BlockJpaRepository blockJpaRepository;
    private final BlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return blockJpaRepository.findTopByOrderByNumberDesc()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public BlocksPage findBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("number").descending());

        Page<BlockEntity> blocksEntityPage = blockJpaRepository.findAll(sortedByBlock);
        long total = blocksEntityPage.getTotalElements();
        int totalPage = blocksEntityPage.getTotalPages();

        List<BlockSummary> blockSummaryList = blocksEntityPage.stream()
                .map(blockEntity -> blockDetailsMapper.toBlockSummary(blockEntity))
                .collect(Collectors.toList());

        return BlocksPage.builder()
                .total(total)
                .totalPages(totalPage)
                .blocks(blockSummaryList)
                .build();
    }

    @Override
    public List<Block> findBlocksByEpoch(int epochNumber) {
        return blockJpaRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> findByBlockHash(String blockHash) {
        return blockJpaRepository.findByHash(blockHash)
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public Optional<Block> findByBlock(long block) {
        return blockJpaRepository.findByNumber(block)
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public int deleteAllBeforeSlot(long slot) {
        return blockJpaRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public void save(Block block) {
        BlockEntity blockEntity = blockDetailsMapper.toBlockEntity(block);
        blockJpaRepository.save(blockEntity);
    }
}
