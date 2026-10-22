package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A chain rollback removes the user tokens first seen after the rollback slot, like {@link Cip68RollbackProcessor} does for the metadata rows. */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cip68UserTokenRollbackProcessor {

    private final Cip68UserTokenRepository repository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = repository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("Rollback to slot {}: deleted {} CIP-68 user token records", rollbackSlot, count);
    }
}
