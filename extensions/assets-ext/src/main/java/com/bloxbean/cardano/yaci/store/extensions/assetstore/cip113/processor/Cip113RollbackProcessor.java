package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class Cip113RollbackProcessor {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = cip113RegistryNodeRepository.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} cip113_registry_node records", count);
    }

}
