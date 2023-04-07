package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockService {
    private final BlockStorage blockStorage;

    public Optional<Block> getBlockByNumber(long blockNumber) {
        return blockStorage.findByBlock(blockNumber);
    }

    public BlocksPage getBlocks(int page, int count) {
        return blockStorage.findBlocks(page, count);
    }
}
