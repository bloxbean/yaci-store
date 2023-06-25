package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionRollbackProcessor {
    private final TransactionStorage transactionStorage;

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = transactionStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} transactions records", count);
    }

}
