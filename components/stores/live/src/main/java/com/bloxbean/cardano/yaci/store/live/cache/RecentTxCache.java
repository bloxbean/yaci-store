package com.bloxbean.cardano.yaci.store.live.cache;

import com.bloxbean.cardano.yaci.store.live.dto.AggregateData;
import com.bloxbean.cardano.yaci.store.live.dto.RecentTx;
import com.google.common.collect.EvictingQueue;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecentTxCache {
    private EvictingQueue<RecentTx> txQueue;
    private AggregateData aggregateData;

    public RecentTxCache() {
        txQueue = EvictingQueue.create(10);
    }

    public void addTx(RecentTx recentTx) {
        txQueue.add(recentTx);
    }

    public List<RecentTx> getRecentTxs() {
        return txQueue.stream().collect(Collectors.toList());
    }

    public void setLastAggregateData(AggregateData aggregateData) {
        this.aggregateData = aggregateData;
    }

    public AggregateData getLastAggregateData() {
        return aggregateData;
    }

}
