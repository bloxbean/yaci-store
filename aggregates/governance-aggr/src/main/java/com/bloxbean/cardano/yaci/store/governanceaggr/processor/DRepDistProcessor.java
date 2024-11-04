package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepStakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DRepDistProcessor {
    private final DRepStakeService dRepStakeService;

    @EventListener
    @Async
    // TODO: manage with job
    public void handleDRepStakeDist(EpochChangeEvent epochChangeEvent) {
        Integer prevEpoch = epochChangeEvent.getPreviousEpoch();

        if (prevEpoch == null) {
            log.info("Previous epoch is null. Skipping handling DRep stake distribution");
            return;
        }

        log.info("Take snapshot DRep stake distribution for epoch {}", prevEpoch);
        dRepStakeService.takeStakeSnapshot(epochChangeEvent.getEventMetadata(), prevEpoch);
    }
}
