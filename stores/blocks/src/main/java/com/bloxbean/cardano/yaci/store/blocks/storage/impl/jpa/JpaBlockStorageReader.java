package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper.JpaBlockMapper;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaBlockEntity;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.repository.JpaBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JpaBlockStorageReader implements BlockStorageReader {

    private final JpaBlockRepository jpaBlockRepository;
    private final JpaBlockMapper blockDetailsMapper;

    @Override
    public Optional<Block> findRecentBlock() {
        return jpaBlockRepository.findTopByOrderByNumberDesc()
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public BlocksPage findBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("number").descending());

        //TODO -- Fix once the count query is fixed
        Slice<JpaBlockEntity> blocksEntityPage = jpaBlockRepository.findAllBlocks(sortedByBlock);
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
        return jpaBlockRepository.findByEpochNumber(epochNumber)
                .stream()
                .map(blockDetailsMapper::toBlock)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Block> findByBlockHash(String blockHash) {
        return jpaBlockRepository.findByHash(blockHash)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public Optional<Block> findByBlock(long block) {
        return jpaBlockRepository.findByNumber(block)
                .map(blockDetailsMapper::toBlock);
    }

    @Override
    public List<PoolBlock> findBlocksBySlotLeaderAndEpoch(String slotLeader, int epoch) {
        return jpaBlockRepository.getBlockEntitiesBySlotLeaderAndEpochNumber(slotLeader, epoch)
                .stream()
                .map(jpaBlockEntity -> PoolBlock.builder()
                        .hash(jpaBlockEntity.getHash())
                        .number(jpaBlockEntity.getNumber())
                        .epoch(jpaBlockEntity.getEpochNumber())
                        .poolId(jpaBlockEntity.getSlotLeader())
                        .build()).collect(Collectors.toList());
    }

}
