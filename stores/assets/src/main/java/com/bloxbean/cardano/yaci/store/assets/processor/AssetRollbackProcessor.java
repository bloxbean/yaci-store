package com.bloxbean.cardano.yaci.store.assets.processor;

import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.assets.AssetsStoreConfiguration.STORE_ASSETS_ENABLED;

@Component
@RequiredArgsConstructor
@Transactional
@EnableIf(STORE_ASSETS_ENABLED)
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


