package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.domain.UtxoRollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConfiguration.STORE_UTXO_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_UTXO_ENABLED)
@Slf4j
public class UtxoRollbackProcessor {
    private final UtxoStorage utxoStorage;
    private final UtxoStoreProperties utxoStoreProperties;
    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long rollBackToSlot = rollbackEvent.getRollbackTo().getSlot();

        if (utxoStoreProperties.isContentAwareRollback()) {
            log.info("Rollback -- content aware rollback is enabled. Publishing UtxoRollbackEvent");
            //Find all addressUtxo records which are greater than rollBackToSlot
            //Find all txInputs which are greater than rollBackToSlot
            var addressUtxos = utxoStorage.getUnspentBySlotGreaterThan(rollBackToSlot);
            var txInputs = utxoStorage.getSpentBySlotGreaterThan(rollBackToSlot);

            log.info("Rollback -- {} address_utxos records", addressUtxos);
            log.info("Rollback -- {} spent output records", txInputs);
            var utxoRollbackEvent = new UtxoRollbackEvent(rollbackEvent, addressUtxos, txInputs);
            publisher.publishEvent(utxoRollbackEvent);
        }

        int deletedUnspent = utxoStorage.deleteUnspentBySlotGreaterThan(rollBackToSlot);
        int deletedSpent = utxoStorage.deleteSpentBySlotGreaterThan(rollBackToSlot);

        log.info("Rollback -- {} address_utxos records", deletedUnspent);
        log.info("Rollback -- {} spent output records", deletedSpent);
    }
}
