package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.service.EpochService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpochProcessor {

    private final EpochService epochService;

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public void scheduleEpochDataAggregation() {
        log.info("Start epoch data aggregation ....");
        epochService.aggregateData();
    }

    //TODO -- handle rollback event
    @EventListener
    @Transactional
    public void handleRollbackEvent(@NotNull RollbackEvent rollbackEvent) {
        log.info("Rollback -- {} block records. //TODO-- Need to handle for EpochProcessor");
    }
}
