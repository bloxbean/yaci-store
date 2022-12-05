package com.bloxbean.cardano.yaci.indexer.protocolparams.processor;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.indexer.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.indexer.protocolparams.service.ProtocolParamService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnBean(LocalClientProvider.class)
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
}
