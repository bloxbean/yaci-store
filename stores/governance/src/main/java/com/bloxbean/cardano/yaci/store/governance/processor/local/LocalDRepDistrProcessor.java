package com.bloxbean.cardano.yaci.store.governance.processor.local;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalDRepDistrService;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalDRepDistrStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@ConditionalOnBean(LocalDRepDistrService.class)
public class LocalDRepDistrProcessor {
    private final LocalDRepDistrService localDRepDistrService;
    private final LocalDRepDistrStorage localDRepDistrStorage;
    private final StoreProperties storeProperties;
    private boolean syncMode = false;

    public LocalDRepDistrProcessor(LocalDRepDistrService localDRepDistrService, LocalDRepDistrStorage localDRepDistrStorage, StoreProperties storeProperties) {

        this.localDRepDistrService = localDRepDistrService;
        this.localDRepDistrStorage = localDRepDistrStorage;
        this.storeProperties = storeProperties;

        if (!storeProperties.isSyncAutoStart()) {
            log.info("Auto sync is disabled. updating local dRep stake distribution will be ignored");
        }
    }

    @EventListener
    public void blockEvent(BlockEvent blockEvent) {
        syncMode = blockEvent.getMetadata().isSyncMode();
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
