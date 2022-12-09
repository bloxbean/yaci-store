package com.bloxbean.cardano.yaci.indexer.blocks.service;

import com.bloxbean.cardano.yaci.indexer.blocks.dto.BlockDetails;
import com.bloxbean.cardano.yaci.indexer.blocks.dto.BlockSummary;
import com.bloxbean.cardano.yaci.indexer.blocks.dto.BlocksPage;
import com.bloxbean.cardano.yaci.indexer.blocks.mapper.BlockDetailsMapper;
import com.bloxbean.cardano.yaci.indexer.blocks.model.BlockEntity;
import com.bloxbean.cardano.yaci.indexer.blocks.repository.BlockRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BlockService {

    private BlockRepository blockRepository;
    private BlockDetailsMapper blockDetailsMapper;

    public Optional<BlockDetails> getBlockByNumber(long blockNumber) {
        return blockRepository.findByBlock(blockNumber)
                .map(blockEntity -> blockDetailsMapper.blockEntityToBlockDetailsMapper(blockEntity));
    }

    public BlocksPage getBlocks(int page, int count) {
        Pageable sortedByBlock =
                PageRequest.of(page, count, Sort.by("block").descending());


        Page<BlockEntity> blocksEntityPage = blockRepository.findAll(sortedByBlock);
        long total = blocksEntityPage.getTotalElements();
        int totalPage = blocksEntityPage.getTotalPages();

        List<BlockSummary> blockSummaryList = blocksEntityPage.stream()
                .map(blockEntity -> blockDetailsMapper.blockEntityToBlockSummaryMapper(blockEntity))
                .collect(Collectors.toList());

        return BlocksPage.builder()
                .total(total)
                .totalPages(totalPage)
                .blocks(blockSummaryList)
                .build();
    }
}
