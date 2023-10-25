package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.service.EpochService;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "store.blocks.epoch-calculation-enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class EpochProcessor {

    private final EpochService epochService;
    private int prevEpoch = -1;

    @Scheduled(fixedRateString = "${store.blocks.epoch-calculation-interval:120}", timeUnit = TimeUnit.SECONDS)
    public void scheduleEpochDataAggregation() {
        log.info("Start epoch data aggregation ....");
        epochService.aggregateData();
    }

    @EventListener
    @Transactional
    @Async
    public void handleBlockHeaderEvent(@NonNull BlockHeaderEvent blockHeaderEvent) {
        aggregateDataDuringEpochChange(blockHeaderEvent.getMetadata());
    }

    @EventListener
    @Transactional
    @Async
    public void handleByronBlockHeaderEvent(ByronMainBlockEvent byronMainBlockEvent) {
        aggregateDataDuringEpochChange(byronMainBlockEvent.getMetadata());
    }

    private void aggregateDataDuringEpochChange(EventMetadata eventMetadata) {
        int currentEpoch = eventMetadata.getEpochNumber();
        if (prevEpoch == currentEpoch)
            return;

        log.info("Epoch changed from {} to {}", prevEpoch, currentEpoch);
        epochService.aggregateData();

        prevEpoch = currentEpoch;
    }

    //TODO -- handle rollback event
    @EventListener
    @Transactional
    public void handleRollbackEvent(@NotNull RollbackEvent rollbackEvent) {
        log.info("Rollback -- {} block records. //TODO-- Need to handle for EpochProcessor");
    }
}
