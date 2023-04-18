package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.service.EpochService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpochProcessor {

    private final EpochService epochService;

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void scheduleEpochDataAggregation() {
        log.info("Start epoch data aggregation ....");
        epochService.aggregateData();
    }
}
