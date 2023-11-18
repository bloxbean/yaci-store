package com.bloxbean.cardano.yaci.store.api.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class BlockMetricsService {
    private final BlockStorageReader blockReader;

    public BlockMetricsService(BlockStorageReader blockReader, MeterRegistry meterRegistry) {
        this.blockReader = blockReader;
        Gauge.builder("yaci.store.block.recent", getLastBlock()).register(meterRegistry);
    }

    private Supplier<Number> getLastBlock() {
        return () -> blockReader.findRecentBlock()
                .map(block -> block.getNumber())
                .orElse(0L);
    }
}
