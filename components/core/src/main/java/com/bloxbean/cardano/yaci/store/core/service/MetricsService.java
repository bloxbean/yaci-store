package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.storage.impl.CursorRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MetricsService {
    private final CursorRepository cursorRepository;
    private final StoreProperties storeProperties;

    public MetricsService(CursorRepository cursorRepository, StoreProperties storeProperties,
                          MeterRegistry meterRegistry) {
        this.cursorRepository = cursorRepository;
        this.storeProperties = storeProperties;
        Gauge.builder("yaci.store.cursor.pos", getTopBlockNo()).register(meterRegistry);
    }

    private Supplier<Number> getTopBlockNo() {
        return () -> cursorRepository.findTopByIdOrderBySlotDesc(storeProperties.getEventPublisherId())
                .map(cursorEntity -> cursorEntity.getBlock())
                .orElse(0L);
    }
}
