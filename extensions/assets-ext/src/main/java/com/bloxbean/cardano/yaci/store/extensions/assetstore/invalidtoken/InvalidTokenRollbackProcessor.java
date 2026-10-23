package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.repository.InvalidTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A chain rollback removes what was found after the rollback slot, like {@code Cip68RollbackProcessor} does for the CIP-68 rows. Rows with
 * no slot (from the CIP-26 registry in Git) are not touched: they do not come from the chain.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidTokenRollbackProcessor {

    private final InvalidTokenRepository repository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = repository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("Rollback to slot {}: deleted {} invalid token records", rollbackSlot, count);
    }
}
