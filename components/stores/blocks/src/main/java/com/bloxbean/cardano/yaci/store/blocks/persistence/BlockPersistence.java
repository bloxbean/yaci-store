package com.bloxbean.cardano.yaci.store.blocks.persistence;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;

import java.util.Optional;

public interface BlockPersistence {
    Optional<Block> findRecentBlock();

    void save(Block block);

    BlocksPage findBlocks(int page, int count);

    Optional<Block> findByBlockHash(String blockHash);

    Optional<Block> findByBlock(long block);

    int deleteAllBeforeSlot(long slot);
}
