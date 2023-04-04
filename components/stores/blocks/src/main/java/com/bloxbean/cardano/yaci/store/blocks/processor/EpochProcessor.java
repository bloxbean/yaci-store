package com.bloxbean.cardano.yaci.store.blocks.processor;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

public class EpochProcessor {

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
