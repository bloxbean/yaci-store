package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalDRepDistrService;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalDRepDistrStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(LocalDRepDistrService.class)
public class LocalDRepDistrProcessor {
    private final LocalDRepDistrService localDRepDistrService;
    private final LocalDRepDistrStorage localDRepDistrStorage;
    private final BlockFetchService blockFetchService;
    private boolean syncMode = false;

    @EventListener
    public void blockEvent(BlockEvent blockEvent) {
        syncMode = blockEvent.getMetadata().isSyncMode();
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        syncMode = epochChangeEvent.getEventMetadata().isSyncMode();
        if (!syncMode)
            return;

        log.info("Epoch change event received. Fetching and updating dRep stake distribution");
        localDRepDistrService.fetchAndSetDRepDistr();
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        if (!syncMode) {
            return;
        }

        long slot = rollbackEvent.getRollbackTo().getSlot();

        int count = localDRepDistrStorage.deleteBySlotGreaterThan(slot);
        log.info("Rollback -- {} local_drep_distr records", count);

        if (count > 0) {
            log.info("Fetching dRep stake distribution after rollback event....");
            localDRepDistrService.fetchAndSetDRepDistr();
        }
    }

}
