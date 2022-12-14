package com.bloxbean.cardano.yaci.store.protocolparams.processor;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.protocolparams.service.ProtocolParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnBean(LocalClientProvider.class)
@Slf4j
public class ProtocolParamsProcessor {
    private long slot = 0;
    private ProtocolParamService protocolParamService;

    public ProtocolParamsProcessor(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    @EventListener
    @Transactional
    public void handleBlockEvent(BlockHeaderEvent blockHeaderEvent) {
        long currentSlot = blockHeaderEvent.getBlockHeader().getHeaderBody().getSlot();
        if (currentSlot - slot < 5000) //Calculate slots in every 1000 slots
            return;

        slot = currentSlot;
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleFetchAndSetCurrentProtocolParams() {
        log.info("Fetching protocol params ....");
        protocolParamService.fetchAndSetCurrentProtocolParams();
    }
}
