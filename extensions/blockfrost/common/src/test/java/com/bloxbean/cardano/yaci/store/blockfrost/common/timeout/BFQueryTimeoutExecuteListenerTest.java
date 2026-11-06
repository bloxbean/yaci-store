package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.h2.jdbcx.JdbcDataSource;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BFQueryTimeoutExecuteListenerTest {

    // ~10^10 rows: runs for minutes unless the statement is cancelled
    private static final String ENDLESS_QUERY =
            "select sum(a.x * b.x) from system_range(1, 100000) a cross join system_range(1, 100000) b";

    /** Records the timeout each statement carries when it starts, after the listener under test ran. */
    static class ProbeListener implements ExecuteListener {
        final List<Integer> timeouts = new ArrayList<>();

        @Override
        public void executeStart(ExecuteContext ctx) {
            try {
                timeouts.add(ctx.statement().getQueryTimeout());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private ProbeListener probe;
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:bf_timeout;DB_CLOSE_DELAY=-1");

        probe = new ProbeListener();
        dsl = DSL.using(new DefaultConfiguration()
                .set(dataSource)
                .set(SQLDialect.H2)
                .set(new DefaultExecuteListenerProvider(new BFQueryTimeoutExecuteListener()),
                        new DefaultExecuteListenerProvider(probe)));
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
    }

    @Test
    void withoutBudget_statementKeepsNoTimeout() {
        // Indexer threads never have a budget: their statements must stay unbounded
        dsl.fetchValue("select 1");

        assertThat(probe.timeouts).containsExactly(0);
    }

    @Test
    void withBudget_statementGetsRemainingTime() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        dsl.fetchValue("select 1");

        assertThat(probe.timeouts).singleElement().satisfies(t -> assertThat(t).isBetween(29, 30));
    }

    @Test
    void smallerExplicitQueryTimeout_isKept() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        dsl.resultQuery("select 1").queryTimeout(15).fetch();

        assertThat(probe.timeouts).containsExactly(15);
    }

    @Test
    void largerExplicitQueryTimeout_isCappedByBudget() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        dsl.resultQuery("select 1").queryTimeout(120).fetch();

        assertThat(probe.timeouts).singleElement().satisfies(t -> assertThat(t).isBetween(29, 30));
    }

    @Test
    void exhaustedBudget_failsBeforeRunningTheStatement() throws InterruptedException {
        BFQueryTimeoutContext.begin(Duration.ofMillis(1));
        Thread.sleep(5);

        assertThatThrownBy(() -> dsl.fetchValue("select 1"))
                .isInstanceOf(QueryTimeoutException.class);

        assertThat(probe.timeouts).isEmpty();
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void longRunningStatement_isCancelledWhenBudgetRunsOut() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        long start = System.nanoTime();
        assertThatThrownBy(() -> dsl.fetchValue(ENDLESS_QUERY))
                .satisfies(e -> assertThat(BFQueryTimeouts.isTimeout(e)).isTrue());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsedMillis).isLessThan(5_000);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }
}
