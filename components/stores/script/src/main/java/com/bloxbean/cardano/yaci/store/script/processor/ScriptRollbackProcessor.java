package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rollbacks transaction_scripts table
 */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScriptRollbackProcessor {

    private final TxScriptStorage txScriptStorage;

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = txScriptStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} transaction_scripts records", count);
    }
}
