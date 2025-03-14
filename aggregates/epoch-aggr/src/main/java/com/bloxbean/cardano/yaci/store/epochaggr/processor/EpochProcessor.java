package com.bloxbean.cardano.yaci.store.epochaggr.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.epochaggr.service.EpochService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.epochaggr.EpochAggrConfiguration.STORE_EPOCHAGGR_ENABLED;

@Slf4j
@Component
@ConditionalOnProperty(name = "store.epoch-aggr.epoch-calculation-enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@EnableIf(value = STORE_EPOCHAGGR_ENABLED, defaultValue = false)
public class EpochProcessor {

    private final EpochService epochService;
    private int prevEpoch = -1;

    @Scheduled(fixedRateString = "${store.epoch-aggr.epoch-calculation-interval:900}", timeUnit = TimeUnit.SECONDS)
    public void scheduleEpochDataAggregation() {
        log.info("Start epoch data aggregation ....");
        epochService.aggregateData();
    }

    @EventListener
    @Transactional
    @Async
    public void handleCommitEvent(CommitEvent commitEvent) {
        aggregateDataDuringEpochChange(commitEvent.getMetadata());
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
