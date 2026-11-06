package com.bloxbean.cardano.yaci.store.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Waits for the database when the app starts before it is reachable, for example when the app and PostgreSQL start
 * together after a host restart and PostgreSQL is still recovering. Without it the first use of the DataSource
 * (Flyway resolving its locations) fails and the app exits.
 * <p>
 * Runs right after the DataSource bean is created, before anything uses it. Waits up to
 * {@code store.db.startup-wait-seconds} (default 300, 0 to turn it off), then lets the startup fail as before.
 */
@Component
@Slf4j
public class DatabaseStartupWait implements BeanPostProcessor, EnvironmentAware {
    static final String WAIT_SECONDS_PROPERTY = "store.db.startup-wait-seconds";
    private static final long RETRY_INTERVAL_MS = 5_000;

    private long waitSeconds = 300;

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
        long deadline = System.currentTimeMillis() + waitSeconds * 1000;
        int attempt = 0;
        while (true) {
            attempt++;
            try (Connection ignored = dataSource.getConnection()) {
                if (attempt > 1)
                    log.info("Database '{}' is reachable after {} attempts", name, attempt);
                return;
            } catch (Exception e) {
                if (System.currentTimeMillis() >= deadline) {
                    log.error("Database '{}' still not reachable after {} s. Giving up.", name, waitSeconds);
                    return; //the startup continues and fails on the first use, as without this wait
                }
                log.warn("Database '{}' not reachable yet ({}). Trying again in {} s ...", name, rootMessage(e),
                        RETRY_INTERVAL_MS / 1000);
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t)
            t = t.getCause();
        return t.getMessage();
    }
}
