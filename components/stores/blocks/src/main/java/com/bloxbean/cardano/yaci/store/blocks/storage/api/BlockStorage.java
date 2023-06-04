package com.bloxbean.cardano.yaci.store.blocks.storage.api;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;

import java.util.List;
import java.util.Optional;

public interface BlockStorage {
    Optional<Block> findRecentBlock();

    void save(Block block);

    BlocksPage findBlocks(int page, int count);

    List<Block> findBlocksByEpoch(int epochNumber);

    Optional<Block> findByBlockHash(String blockHash);

    Optional<Block> findByBlock(long block);

    List<PoolBlock> findBlocksBySlotLeaderAndEpoch(String slotLeader, int epoch);

    int deleteAllBeforeSlot(long slot);
}
