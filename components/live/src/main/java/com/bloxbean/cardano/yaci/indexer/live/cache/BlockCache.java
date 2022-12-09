package com.bloxbean.cardano.yaci.indexer.live.cache;

import com.bloxbean.cardano.yaci.indexer.live.dto.AggregateData;
import com.bloxbean.cardano.yaci.indexer.live.dto.BlockData;
import com.google.common.collect.EvictingQueue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BlockCache {
    private EvictingQueue<BlockData> blocksQueue;
    private AggregateData aggregateData;

    public BlockCache() {
        blocksQueue = EvictingQueue.create(10);
    }

    public void addBlock(BlockData blockData) {
        blocksQueue.add(blockData);
    }

    public List<BlockData> getBlocks() {
        return blocksQueue.stream().collect(Collectors.toList());
    }

    public void setLastAggregateData(AggregateData aggregateData) {
        this.aggregateData = aggregateData;
    }

    public AggregateData getLastAggregateData() {
        return aggregateData;
    }

}
