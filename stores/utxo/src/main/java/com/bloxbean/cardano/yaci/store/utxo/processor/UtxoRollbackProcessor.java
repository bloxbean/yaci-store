package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UtxoRollbackProcessor {
    private final UtxoStorage utxoStorage;
    private final InvalidTransactionStorage invalidTransactionStorage;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long rollBackToSlot = rollbackEvent.getRollbackTo().getSlot();

        int deleted = utxoStorage.deleteBySlotGreaterThan(rollBackToSlot);
        int invalidTxnDeleted = invalidTransactionStorage.deleteBySlotGreaterThan(rollBackToSlot);

        log.info("Rollback -- {} address_utxos records", deleted);
        log.info("Rollback -- {} invalid_transactions records", invalidTxnDeleted);
    }
}
