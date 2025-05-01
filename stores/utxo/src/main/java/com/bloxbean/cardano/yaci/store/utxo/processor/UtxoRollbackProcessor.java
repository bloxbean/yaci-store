package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.annotation.DomainEventListener;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.utxo.UtxoStoreConstant.STORE_UTXO_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_UTXO_ENABLED)
@Slf4j
public class UtxoRollbackProcessor {
    private final UtxoStorage utxoStorage;

    @DomainEventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        long rollBackToSlot = rollbackEvent.getRollbackTo().getSlot();

        int deletedUnspent = utxoStorage.deleteUnspentBySlotGreaterThan(rollBackToSlot);
        int deletedSpent = utxoStorage.deleteSpentBySlotGreaterThan(rollBackToSlot);

        log.info("Rollback -- {} address_utxos records", deletedUnspent);
        log.info("Rollback -- {} spent output records", deletedSpent);
    }
}
