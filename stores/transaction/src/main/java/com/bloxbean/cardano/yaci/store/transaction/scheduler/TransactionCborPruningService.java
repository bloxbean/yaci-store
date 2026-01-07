package com.bloxbean.cardano.yaci.store.transaction.scheduler;

import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for pruning old transaction CBOR data.
 * CBOR data can grow very large, so this service helps manage storage by
 * automatically deleting CBOR data older than the configured retention period.
 */
@Component
@ConditionalOnProperty(
        value = "store.transaction.cbor-pruning-enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@Slf4j
public class TransactionCborPruningService {
    private final TransactionCborStorage transactionCborStorage;
    private final CursorService cursorService;
    private final TransactionStoreProperties transactionStoreProperties;

    private final AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Transaction CBOR Pruning Service Enabled >>");
        log.info("   Retention: {} slots (~{} days)",
                transactionStoreProperties.getCborPruningSafeSlots(),
                transactionStoreProperties.getCborPruningSafeSlots() / 86400);
    }

    @Scheduled(fixedRateString = "${store.transaction.cbor-pruning-interval:86400}", timeUnit = TimeUnit.SECONDS)
    public void handleCborPruning() {
        if (!transactionStoreProperties.isCborPruningEnabled()) {
            return;
        }

        if (isPruning.get()) {
            log.info("Transaction CBOR pruning is already in progress. Skipping this run.");
            return;
        }

        Thread.startVirtualThread(this::deleteOldCborData);
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        if (!transactionStoreProperties.isCborPruningEnabled()) {
            return;
        }

        if (isPruning.get()) {
            log.info("Transaction CBOR pruning is already in progress. Skipping this run.");
            return;
        }

        Thread.startVirtualThread(this::deleteOldCborData);
    }

    private void deleteOldCborData() {
        isPruning.set(true);
        try {
            cursorService.getCursor().ifPresent(cursor -> {
                log.info("Current cursor for CBOR pruning: block={}, slot={}", cursor.getBlock(), cursor.getSlot());

                long retentionSlots = transactionStoreProperties.getCborPruningSafeSlots();
                long pruneBeforeSlot = cursor.getSlot() - retentionSlots;
                
                if (pruneBeforeSlot > 0) {
                    long t1 = System.currentTimeMillis();
                    int deletedCount = transactionCborStorage.deleteBySlotLessThan(pruneBeforeSlot);
                    long t2 = System.currentTimeMillis();
                    
                    log.info("Deleted {} transaction CBOR records before slot {} (retention: {} slots), Time taken: {} ms",
                            deletedCount, pruneBeforeSlot, retentionSlots, (t2 - t1));
                } else {
                    log.debug("No CBOR data to prune yet (current slot: {}, retention: {})", 
                            cursor.getSlot(), retentionSlots);
                }
            });
        } finally {
            isPruning.set(false);
        }
    }
}


