package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.store.repository.CursorRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MetricsService {
    private final CursorRepository cursorRepository;

    @Value("${event.publisher.id:1}")
    private long eventPublisherId;

    public MetricsService(CursorRepository cursorRepository, MeterRegistry meterRegistry) {
        this.cursorRepository = cursorRepository;
        Gauge.builder("yaci.store.cursor.pos", getTopBlockNo()).register(meterRegistry);
    }

    private Supplier<Number> getTopBlockNo() {
        return () -> cursorRepository.findTopByIdOrderByBlockDesc(eventPublisherId)
                .map(cursorEntity -> cursorEntity.getBlock())
                .orElse(0L);
    }
}
