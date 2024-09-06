package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamService;
import jakarta.annotation.PostConstruct;
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
    private StoreProperties storeProperties;

    @PostConstruct
    void init() {
        if (!storeProperties.isSyncAutoStart()) {
            log.info("Auto sync is disabled. updating epoch param will be ignored");
        }
    }

    public LocalEpochParamsScheduler(LocalEpochParamService protocolParamService,StoreProperties storeProperties) {
        this.protocolParamService = protocolParamService;
        this.storeProperties = storeProperties;
    }

    @Scheduled(fixedRateString = "${store.epoch.n2c-protocol-param-fetching-interval-in-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        if (!storeProperties.isSyncAutoStart()) {
            return;
        }
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
