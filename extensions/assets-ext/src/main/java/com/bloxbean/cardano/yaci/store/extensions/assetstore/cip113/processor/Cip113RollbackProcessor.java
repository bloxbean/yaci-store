package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A chain rollback removes the registry node versions found after the rollback slot. Unlike the processor it runs even
 * when CIP-113 is switched off: rows written while it was on must not outlive the blocks they came from.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cip113RollbackProcessor {

    private final Cip113RegistryNodeRepository registryNodeRepository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = registryNodeRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-113 rollback to slot {}: deleted {} registry node records", rollbackSlot, count);
    }
}
