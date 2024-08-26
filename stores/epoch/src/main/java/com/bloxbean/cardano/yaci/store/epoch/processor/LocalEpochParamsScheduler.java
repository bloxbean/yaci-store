package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnBean(LocalEpochParamService.class)
@Slf4j
public class LocalEpochParamsScheduler {
    private LocalEpochParamService protocolParamService;

    public LocalEpochParamsScheduler(LocalEpochParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    @Scheduled(fixedRateString = "${store.epoch.n2c-protocol-param-fetching-interval-in-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
