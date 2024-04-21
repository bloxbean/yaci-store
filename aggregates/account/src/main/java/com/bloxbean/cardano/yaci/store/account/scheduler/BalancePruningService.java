package com.bloxbean.cardano.yaci.store.account.scheduler;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        value="store.account.pruning-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class BalancePruningService {
    private final CursorService cursorService;
    private final AccountStoreProperties accountStoreProperties;
    private final DSLContext dslContext;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private AtomicBoolean isPruning = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("<< Balance Pruning Service Enabled >>");
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedRateString = "${store.account.pruning-interval:86400}", timeUnit = TimeUnit.SECONDS, initialDelay = 60)
    public void handleUtxoPruning() {
        if (accountStoreProperties.isHistoryCleanupEnabled()) {
            if (log.isDebugEnabled())
                log.debug("Account balance pruning is disabled as history cleanup is enabled. Skipping this run !!!");
            return;
        }

        if (isPruning.get()) {
            log.info("Account balance pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(() -> deleteAddressBalance());
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        if (accountStoreProperties.isHistoryCleanupEnabled()) {
            log.info("Account balance pruning is disabled as history cleanup is enabled. Skipping this run !!!");
            return;
        }

        if (isPruning.get()) {
            log.info("Account Balance pruning is already in progress. Skipping this run !!!");
            return;
        }

        Thread.startVirtualThread(() -> deleteAddressBalance());
    }

    private void deleteAddressBalance() {
        isPruning.set(true);
        try {;
            cursorService.getCursor().ifPresent(cursor -> {
                long deleteBlockCount = accountStoreProperties.getBalanceCleanupSlotCount() / 20;
                var block = cursor.getBlock() - deleteBlockCount;

                log.info(">> Pruning account balance records before block: {}", cursor.getBlock() - deleteBlockCount);

                if (block > 0) {
                    long t1 = System.currentTimeMillis();
                    long deleteCount = performAddressBalanceDelete(block);
                    long deleteStakeCount = performStakeAddressBalanceDelete(block);
                    long t2 = System.currentTimeMillis();
                    log.info(">> Deleted {} address_balance, {} stake_address_balance - before block {}, Time taken: {} ms",
                            deleteCount, deleteStakeCount, block, (t2 - t1));
                }
            });
        } finally {
            isPruning.set(false);
        }
    }

    private long performAddressBalanceDelete(long block) {
        int count = 0;
        long totalCount = 0;
        int limit = accountStoreProperties.getPruningBatchSize();

        int repeatCount = 0;
        do {
            count = transactionTemplate.execute(status -> {
                String query = """
                                WITH rows_to_delete AS (SELECT address, unit, slot
                                                        FROM address_balance a1
                                                        WHERE a1.block < ?
                                                          AND EXISTS (SELECT 1
                                                                      FROM address_balance a2
                                                                      WHERE a1.address = a2.address
                                                                        AND a1.unit = a2.unit
                                                                        AND a1.block < a2.block)                                                          
                                                        LIMIT ?
                                                        )
                                delete
                                FROM address_balance
                                WHERE (address, unit, slot) IN (SELECT address, unit, slot FROM rows_to_delete)
                            """;

                return dslContext.execute(query, block, limit);
            });

            totalCount += count;
            if (log.isDebugEnabled())
                log.debug("Deleted {} address_balance records", count);

            repeatCount++;
            if (repeatCount % 20 == 0) {
                log.info("Total address_balance records deleted: {}", totalCount);
            }

        } while (count > 0);

        return totalCount;
    }

    private long performStakeAddressBalanceDelete(long block) {
        int count = 0;
        long totalCount = 0;
        int limit = accountStoreProperties.getPruningBatchSize();

        int repeatCount = 0;
        do {
            count = transactionTemplate.execute(status -> {
                String query = """
                                WITH rows_to_delete AS (SELECT address, slot
                                                        FROM stake_address_balance a1
                                                        WHERE a1.block < ?
                                                          AND EXISTS (SELECT 1
                                                                      FROM stake_address_balance a2
                                                                      WHERE a1.address = a2.address                                                                    
                                                                        AND a1.block < a2.block)                                                          
                                                        LIMIT ?
                                                        )
                                delete
                                FROM stake_address_balance
                                WHERE (address, slot) IN (SELECT address, slot FROM rows_to_delete)
                            """;

                return dslContext.execute(query, block, limit);
            });

            totalCount += count;
            if (log.isDebugEnabled())
                log.debug("Deleted {} stake_address_balance records", count);

            repeatCount++;
            if (repeatCount % 20 == 0) {
                log.info("Total stake_address_balance records deleted: {}", totalCount);
            }

        } while (count > 0);

        return totalCount;
    }
}
