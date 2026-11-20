package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.common.executor.InFlightTransactions;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The balance saves run in a JPA transaction (REQUIRES_NEW, on a virtual thread) and write through jOOQ. The connection
 * that {@link InFlightTransactions} tracks, and aborts on timeout, must be the one jOOQ writes through.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:tracking",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class BalanceSaveTransactionTrackingTest {
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Test
    void tracksTheConnectionJooqWritesThrough_untilTheTransactionCompletes() throws Exception {
        // As Spring Boot's jOOQ auto-configuration builds the DSLContext the storage classes use
        DSLContext dsl = DSL.using(new TransactionAwareDataSourceProxy(dataSource), SQLDialect.H2);
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        InFlightTransactions inFlight = new InFlightTransactions(dataSource);

        CompletableFuture<Integer> trackedDuringTransaction = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            try {
                requiresNew.execute(status -> {
                    inFlight.trackCurrentTransaction();
                    long jooqSession = dsl.fetchValue(DSL.field("session_id()", Long.class));
                    long trackedSession = sessionId(DataSourceUtils.getConnection(dataSource));
                    assertThat(trackedSession).isEqualTo(jooqSession);
                    trackedDuringTransaction.complete(inFlight.trackedCount());
                    return null;
                });
            } catch (Throwable e) {
                trackedDuringTransaction.completeExceptionally(e);
            }
        }).join();

        assertThat(trackedDuringTransaction.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        assertThat(inFlight.trackedCount()).isZero();
    }

    private static long sessionId(java.sql.Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select session_id()")) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
