package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import com.bloxbean.cardano.yaci.store.transaction.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
@EnableIf(TransactionStoreConfiguration.STORE_TRANSACTION_ENABLED)
@Slf4j
public class TransactionRollbackProcessor {
    private final TransactionStorage transactionStorage;
    private final TransactionWitnessStorage transactionWitnessStorage;
    private final InvalidTransactionStorage invalidTransactionStorage;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = transactionStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} transactions records", count);

        count = transactionWitnessStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} transaction_witness records", count);

        count = invalidTransactionStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} invalid_transaction records", count);
    }

}
