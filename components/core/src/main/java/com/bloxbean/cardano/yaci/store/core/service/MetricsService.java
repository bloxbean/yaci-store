package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.storage.impl.jpa.repository.JpaCursorRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MetricsService {
    private final JpaCursorRepository jpaCursorRepository;
    private final StoreProperties storeProperties;

    public MetricsService(JpaCursorRepository jpaCursorRepository, StoreProperties storeProperties,
                          MeterRegistry meterRegistry) {
        this.jpaCursorRepository = jpaCursorRepository;
        this.storeProperties = storeProperties;
        Gauge.builder("yaci.store.cursor.pos", getTopBlockNo()).register(meterRegistry);
    }

    private Supplier<Number> getTopBlockNo() {
        return () -> jpaCursorRepository.findTopByIdOrderBySlotDesc(storeProperties.getEventPublisherId())
                .map(cursorEntity -> cursorEntity.getBlock())
                .orElse(0L);
    }
}
