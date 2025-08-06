package com.bloxbean.cardano.yaci.store.core.metrics;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsService {
    public static final String YACI_STORE_CURRENT_BLOCK = "yaci.store.current.block";
    public static final String YACI_STORE_CURRENT_BLOCK_TIME = "yaci.store.current.block_time";
    public static final String YACI_STORE_CURRENT_EPOCH = "yaci.store.current.epoch";
    public static final String YACI_STORE_CURRENT_ERA = "yaci.store.current.era";
    public static final String YACI_STORE_SYNC_MODE = "yaci.store.sync_mode";
    public static final String YACI_STORE_PROTOCOL_MAGIC = "yaci.store.protocol_magic";
    public static final String YACI_STORE_CURRENT_SLOT = "yaci.store.current.slot";
    public static final String YACI_STORE_LAST_RECEIVED_BLOCK_TIME = "yaci.store.last_received_block_time";

    private AtomicLong currentBlockNo = new AtomicLong(0);
    private AtomicLong currentEpochNo = new AtomicLong(0);
    private AtomicInteger currentEraNo = new AtomicInteger(0);
    private AtomicLong currentBlockTime = new AtomicLong(0);
    private AtomicInteger isSyncMode = new AtomicInteger(0);
    private AtomicLong protocolMagic = new AtomicLong(0);
    private AtomicLong currentSlot = new AtomicLong(0);
    private AtomicLong lastReceivedBlockTime = new AtomicLong(0);

    public MetricsService(MeterRegistry meterRegistry, StoreProperties storeProperties) {

        if (!storeProperties.isMetricsEnabled())
            return;

        Gauge.builder(YACI_STORE_CURRENT_BLOCK, currentBlockNo, AtomicLong::get)
                .description("Current block number being processed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_CURRENT_BLOCK_TIME, currentBlockTime, value -> value.get() * 1000)
                .description("Current block time being processed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_CURRENT_EPOCH, currentEpochNo, AtomicLong::get)
                .description("Current epoch number being processed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_CURRENT_ERA, currentEraNo, AtomicInteger::get)
                .description("Current era number being processed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_SYNC_MODE, isSyncMode, AtomicInteger::get)
                .description("Sync Mode. 0 for Sync in progress, 1 for Sync completed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_PROTOCOL_MAGIC, protocolMagic, AtomicLong::get)
                .description("Protocol magic of the current chain")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_CURRENT_SLOT, currentSlot, AtomicLong::get)
                .description("Current slot being processed")
                .register(meterRegistry);

        Gauge.builder(YACI_STORE_LAST_RECEIVED_BLOCK_TIME, lastReceivedBlockTime, AtomicLong::get)
                .description("Timestamp of the last received block in milliseconds")
                .register(meterRegistry);
    }

    public void updateMetrics(EventMetadata metadata) {

        currentBlockNo.set(metadata.getBlock());
        currentBlockTime.set(metadata.getBlockTime());
        currentEpochNo.set(metadata.getEpochNumber());
        currentEraNo.set(metadata.getEra().getValue());
        isSyncMode.set(metadata.isSyncMode()? 1 : 0);
        protocolMagic.set(metadata.getProtocolMagic());
        currentSlot.set(metadata.getSlot());
    }

    public void updateLastReceivedBlockTime(long timestamp) {
        lastReceivedBlockTime.set(timestamp);
    }
}
