package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.store.epoch.service.LocalProtocolParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnBean(LocalProtocolParamService.class)
@Slf4j
public class ProtocolParamsProcessor {
    private LocalProtocolParamService protocolParamService;

    public ProtocolParamsProcessor(LocalProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
