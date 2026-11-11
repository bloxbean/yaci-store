package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UnspentUtxoTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maintains {@code address_utxo_unspent} when {@code store.utxo.unspent-table-enabled} is on.
 * See {@link UnspentUtxoTableService} for why it works per commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnspentUtxoTableProcessor {

    private final UtxoStoreProperties properties;
    private final UnspentUtxoTableService service;

    /**
     * After every other commit listener, in particular {@code UtxoProcessor.handleCommit}, which
     * may still rewrite outputs (pointer-address stake fields) in {@code address_utxo}.
     */
    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void handleCommit(CommitEvent<?> commitEvent) {
        if (!properties.isUnspentTableEnabled()) {
            return;
        }
        long from = commitEvent.getMetadata().getSlot();
        long to = from;
        List<?> blocks = commitEvent.getBlockCaches();
        if (blocks != null) {
            for (Object block : blocks) {
                if (block instanceof BatchEvent batchEvent && batchEvent.getMetadata() != null) {
                    long slot = batchEvent.getMetadata().getSlot();
                    from = Math.min(from, slot);
                    to = Math.max(to, slot);
                }
            }
        }
        int[] result = service.applyCommit(from, to);
        if (log.isDebugEnabled()) {
            log.debug("address_utxo_unspent: slots {}..{} copied {}, removed {}", from, to, result[0], result[1]);
        }
    }

    /** Before {@code UtxoRollbackProcessor}, which deletes the tx_input rows this restores from. */
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleRollback(RollbackEvent rollbackEvent) {
        if (!properties.isUnspentTableEnabled()) {
            return;
        }
        long slot = rollbackEvent.getRollbackTo().getSlot();
        int[] result = service.rollbackTo(slot);
        log.info("Rollback -- address_utxo_unspent: {} removed, {} restored", result[0], result[1]);
    }
}
