package com.bloxbean.cardano.yaci.store.assets.processor;

import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssetRollbackProcessor {

    private final AssetStorage assetStorage;

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = assetStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} transaction_assets records", count);
    }
}


