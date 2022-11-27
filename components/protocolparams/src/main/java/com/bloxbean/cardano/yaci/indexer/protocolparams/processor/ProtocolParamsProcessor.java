package com.bloxbean.cardano.yaci.indexer.protocolparams.processor;

import com.bloxbean.cardano.yaci.core.helpers.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.indexer.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.indexer.protocolparams.service.ProtocolParamService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(LocalStateQueryClient.class)
public class ProtocolParamsProcessor {
    private long slot = 0;
    private ProtocolParamService protocolParamService;

    public ProtocolParamsProcessor(ProtocolParamService protocolParamService) {
        this.protocolParamService = protocolParamService;
    }

    @EventListener
    public void handleBlockEvent(BlockHeaderEvent blockHeaderEvent) {
        long currentSlot = blockHeaderEvent.getBlockHeader().getHeaderBody().getSlot();
        if (currentSlot - slot < 5000) //Calculate slots in every 1000 slots
            return;

        protocolParamService.fetchAndSetCurrentProtocolParams();
        slot = currentSlot;
    }
}
