package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class BlockMetricsService {
    private final BlockPersistence blockPersistence;

    public BlockMetricsService(BlockPersistence blockPersistence, MeterRegistry meterRegistry) {
        this.blockPersistence = blockPersistence;
        Gauge.builder("yaci.store.block.recent", getLastBlock()).register(meterRegistry);
    }

    private Supplier<Number> getLastBlock() {
        return () -> blockPersistence.findRecentBlock()
                .map(block -> block.getBlock())
                .orElse(0L);
    }
}
