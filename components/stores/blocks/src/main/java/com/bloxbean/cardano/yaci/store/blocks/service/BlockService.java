package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockService {
    private final BlockPersistence blockPersistence;

    public Optional<Block> getBlockByNumber(long blockNumber) {
        return blockPersistence.findByBlock(blockNumber);
    }

    public BlocksPage getBlocks(int page, int count) {
        return blockPersistence.findBlocks(page, count);
    }
}
