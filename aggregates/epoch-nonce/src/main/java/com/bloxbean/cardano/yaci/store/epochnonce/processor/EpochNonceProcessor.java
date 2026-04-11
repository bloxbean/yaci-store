package com.bloxbean.cardano.yaci.store.epochnonce.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.epochnonce.service.EpochNonceService;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bloxbean.cardano.yaci.store.epochnonce.EpochNonceConfiguration.STORE_EPOCH_NONCE_ENABLED;

@Component
@RequiredArgsConstructor
@ReadOnly(false)
@EnableIf(value = STORE_EPOCH_NONCE_ENABLED, defaultValue = false)
@Slf4j
public class EpochNonceProcessor {
    private final EpochNonceService epochNonceService;

    @EventListener
    @Transactional
    public void handleEpochTransition(EpochTransitionCommitEvent event) {
        // Handle null previousEpoch (restart case) — like AdaPotProcessor
        if (event.getPreviousEpoch() == null && event.getEpoch() > 0) {
            return;
        }

        // Skip Byron era — no VRF/nonce
        if (event.getEra() == Era.Byron) {
            return;
        }

        epochNonceService.computeEpochNonce(event.getEpoch(), event.getPreviousEpoch(), event.getMetadata());
    }

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        epochNonceService.rollback(rollbackEvent.getRollbackTo().getSlot());
    }
}
