package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.storage.RollbackStorage;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.blocks.BlocksStoreConfiguration.STORE_BLOCKS_ENABLED;

@Component
@Transactional
@RequiredArgsConstructor
@EnableIf(STORE_BLOCKS_ENABLED)
@Slf4j
public class RollbackProcessor {
    private final RollbackStorage rollbackStorage;

    @EventListener
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        rollbackStorage.save(rollbackEvent);
    }

}
