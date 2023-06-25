package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class BlockMetricsService {
    private final BlockStorage blockStorage;

    public BlockMetricsService(BlockStorage blockStorage, MeterRegistry meterRegistry) {
        this.blockStorage = blockStorage;
        Gauge.builder("yaci.store.block.recent", getLastBlock()).register(meterRegistry);
    }

    private Supplier<Number> getLastBlock() {
        return () -> blockStorage.findRecentBlock()
                .map(block -> block.getNumber())
                .orElse(0L);
    }
}
