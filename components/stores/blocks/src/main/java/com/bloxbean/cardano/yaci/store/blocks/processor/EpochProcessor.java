package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.service.EpochService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EpochProcessor {

    private EpochService epochService;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleEpochDataAggregation() {
        log.info("Start epoch data aggregation ....");
        epochService.aggregateData();
    }
}
