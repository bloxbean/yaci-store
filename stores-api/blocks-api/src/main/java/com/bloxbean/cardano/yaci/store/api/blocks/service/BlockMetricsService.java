package com.bloxbean.cardano.yaci.store.api.blocks.service;

import com.bloxbean.cardano.yaci.store.api.blocks.storage.BlockReader;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class BlockMetricsService {
    private final BlockReader blockReader;

    public BlockMetricsService(BlockReader blockReader, MeterRegistry meterRegistry) {
        this.blockReader = blockReader;
        Gauge.builder("yaci.store.block.recent", getLastBlock()).register(meterRegistry);
    }

    private Supplier<Number> getLastBlock() {
        return () -> blockReader.findRecentBlock()
                .map(block -> block.getNumber())
                .orElse(0L);
    }
}
