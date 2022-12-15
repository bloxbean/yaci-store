package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.store.blocks.persistence.RollbackPersistence;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RollbackProcessor {
    private final RollbackPersistence rollbackPersistence;

    @EventListener
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        rollbackPersistence.save(rollbackEvent);
    }

}
