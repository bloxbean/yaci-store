package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
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
    private StoreProperties storeProperties;

    public LocalEpochParamsScheduler(LocalEpochParamService protocolParamService,StoreProperties storeProperties) {
        this.protocolParamService = protocolParamService;
        this.storeProperties = storeProperties;

        if (!storeProperties.isSyncAutoStart()) {
            log.info("Auto sync is disabled. updating epoch param will be ignored");
        }
    }

    @Scheduled(fixedRateString = "${store.epoch.n2c-protocol-param-fetching-interval-in-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        if (!storeProperties.isSyncAutoStart()) {
            return;
        }

        if (protocolParamService.getEra() != null && protocolParamService.getEra().value >= Era.Conway.value) {
            try {
                log.info("Fetching protocol params ....");
                protocolParamService.fetchAndSetCurrentProtocolParams();
            } catch (Exception e) {
                log.error("Fetching local protocol params failed", e);
            }
        }
    }
}
