package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warns at startup when PostgreSQL's max_locks_per_transaction is below the recommended value.
 *
 * <p>reward, epoch_stake and drep_dist are partitioned by epoch. A query that spans many epochs
 * locks every partition and partition index it touches, so with the PostgreSQL default (64) such
 * queries fail with "out of shared memory" once the chain has enough epochs.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresLockLimitChecker {
    static final int RECOMMENDED_MAX_LOCKS_PER_TRANSACTION = 1024;

    private final DSLContext dsl;

    @EventListener(ApplicationReadyEvent.class)
    public void checkMaxLocksPerTransaction() {
        if (dsl.dialect().family() != SQLDialect.POSTGRES)
            return;

        try {
            Integer maxLocks = fetchMaxLocksPerTransaction();
            if (maxLocks != null && maxLocks < RECOMMENDED_MAX_LOCKS_PER_TRANSACTION) {
                log.warn("PostgreSQL max_locks_per_transaction is {} (recommended: {}). Queries over many epochs on the partitioned " +
                                "reward, epoch_stake and drep_dist tables can fail with 'out of shared memory'. Increase it and restart PostgreSQL.",
                        maxLocks, RECOMMENDED_MAX_LOCKS_PER_TRANSACTION);
            }
        } catch (Exception e) {
            log.debug("Could not read PostgreSQL max_locks_per_transaction", e);
        }
    }

    Integer fetchMaxLocksPerTransaction() {
        Object value = dsl.fetchValue("show max_locks_per_transaction");
        return value != null ? Integer.valueOf(value.toString().trim()) : null;
    }
}
