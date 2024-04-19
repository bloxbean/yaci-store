package com.bloxbean.cardano.yaci.store.utxo.scheduler;

import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
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

@Component
@ConditionalOnProperty(
        value="store.utxo.pruning-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class UtxoPruningService {
    private final UtxoStorage utxoStorage;
    private final CursorService cursorService;
    private final UtxoStoreProperties utxoStoreProperties;

    private AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Utxo Pruning Service Enabled >>");
    }

    @Scheduled(fixedRateString = "${store.utxo.pruning-interval:1440}", timeUnit = TimeUnit.MINUTES)
    public void handleUtxoPruning() {
        if (isPruning.get()) {
            log.info("Utxo pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(() -> deleteSpentUtxos());
    }

    @EventListener
    @Transactional
    public void handleCommitEvent(EpochChangeEvent epochChangeEvent) {
        if (isPruning.get()) {
            log.info("Utxo pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(() -> deleteSpentUtxos());
    }

    private void deleteSpentUtxos() {
        isPruning.set(true);
        try {;
            cursorService.getCursor().ifPresent(cursor -> {
                log.info("Current cursor: {}", cursor.getBlock());

                var block = cursor.getBlock() - utxoStoreProperties.getPruningSafeBlocks();
                if (block > 0) {
                    long t1 = System.currentTimeMillis();
                    var deleteCount =
                            utxoStorage.deleteBySpentAndBlockLessThan(block);
                    long t2 = System.currentTimeMillis();
                    log.info("Deleted {} spent utxos before block {}, Time taken: {} ms", deleteCount, block, (t2 - t1));
                }
            });
        } finally {
            isPruning.set(false);
        }
    }
}
