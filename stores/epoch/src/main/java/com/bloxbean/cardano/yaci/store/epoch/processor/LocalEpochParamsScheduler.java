package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;


@Slf4j
public class LocalEpochParamsScheduler {
    private LocalEpochParamService protocolParamService;

    public LocalEpochParamsScheduler(LocalEpochParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
        log.info("<< LocalEpochParamsScheduler initialized >>>");
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
