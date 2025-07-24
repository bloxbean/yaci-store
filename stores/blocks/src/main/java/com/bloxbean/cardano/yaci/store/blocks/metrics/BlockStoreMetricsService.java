package com.bloxbean.cardano.yaci.store.blocks.metrics;

import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BlockStoreMetricsService {
    public static final String YACI_STORE_CURRENT_DB_BLOCK = "yaci.store.current.db.block";
    public static final String YACI_STORE_CURRENT_DB_EPOCH = "yaci.store.current.db.epoch";

    private final BlockStorageReader blockReader;
    private final BlocksStoreProperties blocksStoreProperties;
    private BlockStoreMetricsFetcher blockStoreMetricsFetcher;

    public BlockStoreMetricsService(BlockStorageReader blockReader, MeterRegistry meterRegistry, BlocksStoreProperties blocksStoreProperties) {
        this.blockReader = blockReader;
        this.blocksStoreProperties = blocksStoreProperties;
        this.blockStoreMetricsFetcher = new BlockStoreMetricsFetcher();

        if (blocksStoreProperties.isMetricsEnabled()) {
            Gauge.builder(YACI_STORE_CURRENT_DB_BLOCK, () -> blockStoreMetricsFetcher.getLastBlockNumber()).register(meterRegistry);
            Gauge.builder(YACI_STORE_CURRENT_DB_EPOCH, () -> blockStoreMetricsFetcher.getLastEpochNumber()).register(meterRegistry);
            log.info("BlockStoreMetricsService initialized with metrics enabled.");
        }
    }

    public class BlockStoreMetricsFetcher {
        private long lastBlockNumber;
        private int lastEpochNumber;
        private long lastUpdatedTimeMillis = 0L;

        public long getLastBlockNumber() {
            updateBlockData();
            return lastBlockNumber;
        }

        public long getLastEpochNumber() {
            updateBlockData();
            return lastEpochNumber;
        }

        public synchronized void updateBlockData() {
            try {
                long now = System.currentTimeMillis();
                if (now - lastUpdatedTimeMillis > blocksStoreProperties.getMetricsUpdateInterval()) {
                    if (log.isTraceEnabled())
                        log.trace("Updating block metrics...");
                    var block = blockReader.findRecentBlock().orElse(null);
                    if (block != null) {
                        lastBlockNumber = block.getNumber();
                        lastEpochNumber = block.getEpochNumber();
                    }
                    lastUpdatedTimeMillis = now;
                }
            } catch (Exception e) {
                log.error("Error while updating block metrics", e);
            }
        }
    }
}
