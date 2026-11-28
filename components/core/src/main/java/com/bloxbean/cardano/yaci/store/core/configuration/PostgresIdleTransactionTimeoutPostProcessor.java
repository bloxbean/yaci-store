package com.bloxbean.cardano.yaci.store.core.configuration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Makes PostgreSQL end a session of the application's connection pool that stays idle inside an open transaction for
 * longer than {@value #PROPERTY} seconds (default {@value #DEFAULT_SECONDS}, 30 minutes). The server rolls the
 * transaction back and releases its locks.
 *
 * <p>Long queries are not affected: the timeout only counts time in which the session waits for the application
 * with a transaction open. It is a last line of defence against a thread that stops in the middle of a transaction,
 * for example inside the JDBC driver. Such a transaction keeps its row locks, and every sync session that auto-recovery
 * starts afterwards waits behind them until the pool is exhausted, so without this sync never resumes.
 *
 * <p>Applied through HikariCP's {@code connectionInitSql}, only to pools with a {@code jdbc:postgresql:} URL and only
 * when no {@code connectionInitSql} is configured. 0 or a negative value turns it off.
 */
@Slf4j
public class PostgresIdleTransactionTimeoutPostProcessor implements BeanPostProcessor {
    public static final String PROPERTY = "store.db.idle-in-transaction-session-timeout-seconds";
    public static final long DEFAULT_SECONDS = 1800;

    private final long timeoutSeconds;

    public PostgresIdleTransactionTimeoutPostProcessor(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (timeoutSeconds <= 0 || !(bean instanceof HikariDataSource dataSource))
            return bean;

        String jdbcUrl = dataSource.getJdbcUrl();
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:"))
            return bean;

        if (dataSource.getConnectionInitSql() != null && !dataSource.getConnectionInitSql().isBlank()) {
            log.info("Data source '{}' has its own connection-init-sql, so {} is not applied to it. Add "
                    + "'SET idle_in_transaction_session_timeout = ...' there to keep the protection.", beanName, PROPERTY);
            return bean;
        }

        try {
            dataSource.setConnectionInitSql("SET idle_in_transaction_session_timeout = '" + timeoutSeconds + "s'");
            log.info("PostgreSQL sessions of data source '{}' end after {} s idle inside a transaction ({})",
                    beanName, timeoutSeconds, PROPERTY);
        } catch (IllegalStateException e) {
            // The pool has already started, so its configuration is sealed
            log.warn("Could not set idle_in_transaction_session_timeout on data source '{}': {}", beanName, e.getMessage());
        }
        return bean;
    }
}
