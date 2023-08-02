package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockService {
    private final BlockStorage blockStorage;

    public Optional<Block> getBlockByNumber(long blockNumber) {
        return blockStorage.findByBlock(blockNumber);
    }

    public Optional<Block> getBlockByHash(String blockHash) {
        return blockStorage.findByBlockHash(blockHash);
    }

    public BlocksPage getBlocks(int page, int count) {
        return blockStorage.findBlocks(page, count);
    }

    public List<PoolBlock> getBlocksBySlotLeaderEpoch(String slotLeader, int epoch) {
        return blockStorage.findBlocksBySlotLeaderAndEpoch(slotLeader, epoch);
    }

    public Optional<Block> getLatestBlock() {
        return blockStorage.findRecentBlock();
    }
}
