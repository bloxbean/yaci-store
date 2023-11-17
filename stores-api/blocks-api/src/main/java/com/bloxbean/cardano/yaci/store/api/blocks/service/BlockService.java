package com.bloxbean.cardano.yaci.store.api.blocks.service;

import com.bloxbean.cardano.yaci.store.api.blocks.storage.BlockReader;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlockService {
    private final BlockReader blockReader;

    public Optional<Block> getBlockByNumber(long blockNumber) {
        return blockReader.findByBlock(blockNumber);
    }

    public Optional<Block> getBlockByHash(String blockHash) {
        return blockReader.findByBlockHash(blockHash);
    }

    public BlocksPage getBlocks(int page, int count) {
        return blockReader.findBlocks(page, count);
    }

    public List<PoolBlock> getBlocksBySlotLeaderEpoch(String slotLeader, int epoch) {
        return blockReader.findBlocksBySlotLeaderAndEpoch(slotLeader, epoch);
    }

    public Optional<Block> getLatestBlock() {
        return blockReader.findRecentBlock();
    }
}
