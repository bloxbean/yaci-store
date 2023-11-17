package com.bloxbean.cardano.yaci.store.blocks.storage.api;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;

import java.util.List;
import java.util.Optional;

public interface BlockStorage {
    Optional<Block> findRecentBlock();
    void save(Block block);
    List<Block> findBlocksByEpoch(int epochNumber);
    int deleteBySlotGreaterThan(long slot);
}
