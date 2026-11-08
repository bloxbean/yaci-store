package com.bloxbean.cardano.yaci.store.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.LongSupplier;

/**
 * Waits for the database when the app starts before it is reachable, for example when the app and PostgreSQL start
 * together after a host restart and PostgreSQL is still recovering. Without it the first use of the DataSource
 * (Flyway resolving its locations) fails and the app exits.
 * <p>
 * Runs right after the DataSource bean is created, before anything uses it. Retries every 5 s for up to
 * {@code store.db.startup-wait-seconds} (default 300, 0 to turn it off), then lets the startup fail as before.
 * The last pause ends at the deadline, so the last attempt starts at the deadline and no attempt starts after it.
 * <p>
 * The budget does not cut short a single {@code getConnection()} call that hangs (for example a host that drops
 * packets). That call is bounded by the pool/driver connection timeout instead
 * ({@code spring.datasource.hikari.connection-timeout}, 30 s by default), so the total wait can exceed the budget
 * by up to one such timeout.
 */
@Component
@Slf4j
public class DatabaseStartupWait implements BeanPostProcessor, EnvironmentAware {
    static final String WAIT_SECONDS_PROPERTY = "store.db.startup-wait-seconds";
    static final long RETRY_INTERVAL_MS = 5_000;

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final LongSupplier clockMillis;
    private final Sleeper sleeper;
    private long waitSeconds = 300;

    public DatabaseStartupWait() {
        this(() -> System.nanoTime() / 1_000_000, Thread::sleep);
    }

    //for tests
    DatabaseStartupWait(LongSupplier clockMillis, Sleeper sleeper) {
        this.clockMillis = clockMillis;
        this.sleeper = sleeper;
    }

    @Override
    public void setEnvironment(Environment environment) {
        waitSeconds = environment.getProperty(WAIT_SECONDS_PROPERTY, Long.class, 300L);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && waitSeconds > 0)
            waitFor(dataSource, beanName);
        return bean;
    }

    void waitFor(DataSource dataSource, String name) {
        long deadline = clockMillis.getAsLong() + waitSeconds * 1000;
        int attempt = 0;
        while (true) {
            attempt++;
            try (Connection ignored = dataSource.getConnection()) {
                if (attempt > 1)
                    log.info("Database '{}' is reachable after {} attempts", name, attempt);
                return;
            } catch (Exception e) {
                long remaining = deadline - clockMillis.getAsLong();
                if (remaining <= 0) {
                    giveUp(name, attempt);
                    return;
                }
                long pause = Math.min(RETRY_INTERVAL_MS, remaining);
                log.warn("Database '{}' not reachable yet ({}). Trying again in {} ms ...", name, rootMessage(e), pause);
                try {
                    sleeper.sleep(pause);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for database '{}'. Not waiting any longer.", name);
                    return;
                }
                //the pause ends at the deadline at the latest, so the next attempt starts within the budget
            }
        }
    }

    private void giveUp(String name, int attempts) {
        //the startup continues and fails on the first use, as without this wait
        log.error("Database '{}' still not reachable after {} s ({} attempts). Giving up.", name, waitSeconds, attempts);
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t)
            t = t.getCause();
        return t.getMessage();
    }
}
