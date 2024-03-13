package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.BlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.BlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BlockStorageReaderImpl implements BlockStorageReader {

    private final BlockRepository blockRepository;
    private final BlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return blockRepository.findTopByOrderByNumberDesc()
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public BlocksPage findBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("number").descending());

        //TODO -- Fix once the count query is fixed
        Slice<BlockEntity> blocksEntityPage = blockRepository.findAllBlocks(sortedByBlock);
//      long total = blocksEntityPage.getTotalElements();
//      int totalPage = blocksEntityPage.getTotalPages();

        List<BlockSummary> blockSummaryList = blocksEntityPage.stream()
                .map(blockEntity -> blockDetailsMapper.toBlockSummary(blockEntity))
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
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> findByBlockHash(String blockHash) {
        return blockRepository.findByHash(blockHash)
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public Optional<Block> findByBlock(long block) {
        return blockRepository.findByNumber(block)
                .map(blockEntity -> blockDetailsMapper.toBlock(blockEntity));
    }

    @Override
    public List<PoolBlock> findBlocksBySlotLeaderAndEpoch(String slotLeader, int epoch) {
        return blockRepository.getBlockEntitiesBySlotLeaderAndEpochNumber(slotLeader, epoch)
                .stream()
                .map(blockEntity -> PoolBlock.builder()
                        .hash(blockEntity.getHash())
                        .number(blockEntity.getNumber())
                        .epoch(blockEntity.getEpochNumber())
                        .poolId(blockEntity.getSlotLeader())
                        .build()).collect(Collectors.toList());
    }

}
