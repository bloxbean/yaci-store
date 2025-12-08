package com.bloxbean.cardano.yaci.store.transaction.scheduler;


import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
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

import static com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration.STORE_TRANSACTION_PRUNING_ENABLED;

@Component
@ConditionalOnProperty(
        value = "store.transaction.pruning-enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
@EnableIf(STORE_TRANSACTION_PRUNING_ENABLED)
@Slf4j
public class TransactionPruningService {
    private final TransactionStorage transactionStorage;
    private final TransactionWitnessStorage transactionWitnessStorage;
    private final TransactionCborStorage transactionCborStorage;
    private final CursorService cursorService;
    private final TransactionStoreProperties transactionStoreProperties;

    private final AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Transaction Pruning Service Enabled >>");
    }

    @Scheduled(fixedRateString = "${store.transaction.pruning-interval:86400}", timeUnit = TimeUnit.SECONDS)
    public void handleTransactionPruning() {
        if (!transactionStoreProperties.isPruningEnabled()) {
            return;
        }

        if (isPruning.get()) {
            log.info("Transaction pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(this::deleteOldTransactions);
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
            if (isPruning.get()) {
            log.info("Transaction pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(this::deleteOldTransactions);
    }

    private void deleteOldTransactions() {
        isPruning.set(true);
        try {
            cursorService.getCursor().ifPresent(cursor -> {
                log.info("Current cursor: {}", cursor.getBlock());

                var slot = cursor.getSlot() - transactionStoreProperties.getPruningSafeSlots();
                if (slot > 0) {
                    long t1 = System.currentTimeMillis();
                    var deleteTxCount =
                            transactionStorage.deleteBySlotLessThan(slot);
                    var deleteTxWitnessCount = transactionWitnessStorage.deleteBySlotLessThan(slot);
                    var deleteTxCborCount = transactionCborStorage.deleteBySlotLessThan(slot);
                    // skip pruning invalid_transaction and withdrawn because these 2 tables do not have too much data
                    long t2 = System.currentTimeMillis();
                    log.info("Deleted {} transactions and {} transaction witnesses before slot {}, Time taken: {} ms",
                            deleteTxCount, deleteTxWitnessCount, slot, (t2 - t1));
                    if (deleteTxCborCount > 0) {
                        log.info("Deleted {} transaction_cbor records before slot {}", deleteTxCborCount, slot);
                    }
                }
            });
        } finally {
            isPruning.set(false);
        }
    }
}
