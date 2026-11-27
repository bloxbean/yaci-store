package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFDataAccessExceptionHandler;
import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFGlobalExceptionHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BFQueryTimeoutDataSource} over a real Hikari pool of one connection, so connection waits and connection
 * reuse are the pool's own behaviour.
 */
class BFQueryTimeoutDataSourceTest {

    // ~10^10 rows: runs for minutes unless the statement is cancelled
    private static final String ENDLESS_QUERY = "select count(*) from system_range(1, 100000) a "
            + "cross join system_range(1, 100000) b where a.x + b.x < 0";
    private static final String INSERT = "insert into bf_ds_row(id) values (1)";

    private HikariDataSource pool;
    private BFQueryTimeoutDataSource dataSource;
    private JdbcTemplate withoutBudget;

    @BeforeEach
    void setUp() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:bf_ds_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        HikariConfig config = new HikariConfig();
        config.setDataSource(RecordingDataSource.wrap(h2));
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(10_000);
        pool = new HikariDataSource(config);
        dataSource = new BFQueryTimeoutDataSource(pool);

        withoutBudget = new JdbcTemplate(dataSource);
        withoutBudget.execute("create table bf_ds_row(id int)");
        RecordingDataSource.reset();
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
        RecordingDataSource.reset();
        pool.close();
    }

    private int rows() {
        return withoutBudget.queryForObject("select count(*) from bf_ds_row", Integer.class);
    }

    @Test
    void withoutBudget_connectionIsThePoolsOwnAndHasNoTimeout() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertThat(Proxy.isProxyClass(connection.getClass())).isFalse();
            statement.executeQuery("select 1").close();
        }

        assertThat(RecordingDataSource.last().timeoutSeconds()).isZero();
    }

    @Test
    void withBudget_statementGetsRemainingTime() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.executeQuery().close();
        }

        assertThat(RecordingDataSource.last().timeoutSeconds()).isBetween(29, 30);
    }

    @Test
    void smallerCallerTimeout_isKeptAndRestored() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.setQueryTimeout(15);
            statement.executeQuery().close();

            assertThat(RecordingDataSource.last().timeoutSeconds()).isEqualTo(15);
            assertThat(statement.getQueryTimeout()).isEqualTo(15);
        }
    }

    @Test
    void largerCallerTimeout_isCappedByBudget() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.setQueryTimeout(120);
            statement.executeQuery().close();
        }

        assertThat(RecordingDataSource.last().timeoutSeconds()).isBetween(29, 30);
    }

    @Test
    void jooqExplicitTimeout_isKept() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        DSL.using(dataSource, SQLDialect.H2).resultQuery("select 1").queryTimeout(15).fetch();

        assertThat(RecordingDataSource.last().timeoutSeconds()).isEqualTo(15);
    }

    @Test
    void exhaustedBudget_statementIsNotRun() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ZERO);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            assertThatThrownBy(statement::executeUpdate).isInstanceOf(QueryTimeoutException.class);
        }

        assertThat(RecordingDataSource.EXECUTED).isEmpty();
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
        BFQueryTimeoutContext.clear();
        assertThat(rows()).isZero();
    }

    @Test
    void exhaustedBudget_batchIsNotRun() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ZERO);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.addBatch();
            statement.addBatch();
            assertThatThrownBy(statement::executeBatch).isInstanceOf(QueryTimeoutException.class);
        }

        assertThat(RecordingDataSource.EXECUTED).isEmpty();
        BFQueryTimeoutContext.clear();
        assertThat(rows()).isZero();
    }

    @Test
    void jooqExhaustedBudget_isQueryTimeoutException() {
        BFQueryTimeoutContext.begin(Duration.ZERO);

        assertThatThrownBy(() -> DSL.using(dataSource, SQLDialect.H2).fetchValue("select 1"))
                .isInstanceOf(QueryTimeoutException.class);
    }

    @Test
    void longRunningStatement_isCancelledWhenBudgetRunsOut() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(1));

        long start = System.nanoTime();
        assertThatThrownBy(() -> new JdbcTemplate(dataSource).queryForObject(ENDLESS_QUERY, Long.class))
                .satisfies(e -> assertThat(BFQueryTimeouts.isTimeout(e)).isTrue());

        assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(5_000);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void sameStatementAfterBudgetEnds_getsCallerTimeoutAgain() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.executeQuery().close();
            assertThat(RecordingDataSource.last().timeoutSeconds()).isBetween(29, 30);

            BFQueryTimeoutContext.clear();
            statement.executeQuery().close();
            assertThat(RecordingDataSource.last().timeoutSeconds()).isZero();
        }
    }

    @Test
    void connectionReusedByThreadWithoutBudget_keepsNoTimeout() throws Exception {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery("select 1").close();
        }
        BFQueryTimeoutContext.clear();
        RecordingDataSource.Executed inRequest = RecordingDataSource.last();

        Thread background = new Thread(() -> withoutBudget.queryForObject("select 1", Integer.class));
        background.start();
        background.join();

        RecordingDataSource.Executed inBackground = RecordingDataSource.last();
        assertThat(inBackground.connectionId()).isEqualTo(inRequest.connectionId());
        assertThat(inRequest.timeoutSeconds()).isBetween(29, 30);
        assertThat(inBackground.timeoutSeconds()).isZero();
    }

    @Test
    void unwrap_reachesThePool() throws Exception {
        assertThat(dataSource.isWrapperFor(HikariDataSource.class)).isTrue();
        assertThat(dataSource.unwrap(HikariDataSource.class)).isSameAs(pool);
    }

    @Test
    void postProcessor_wrapsOnlyTheMainDataSource() {
        BFQueryTimeoutDataSourcePostProcessor postProcessor = new BFQueryTimeoutDataSourcePostProcessor();

        assertThat(postProcessor.postProcessAfterInitialization(pool, "dataSource"))
                .isInstanceOf(BFQueryTimeoutDataSource.class);
        assertThat(postProcessor.postProcessAfterInitialization(pool, "duckDbWriterDataSource")).isSameAs(pool);
        assertThat(postProcessor.postProcessAfterInitialization(dataSource, "dataSource")).isSameAs(dataSource);
    }

    // Lives under com.bloxbean.cardano.yaci.store.blockfrost, so the interceptor and advices apply.
    @RestController
    static class BfController {
        private final DataSource dataSource;

        BfController(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @GetMapping("/bf/insert")
        int insert() {
            return new JdbcTemplate(dataSource).update(INSERT);
        }
    }

    @Test
    void budgetSpentWaitingForAConnection_statementIsNotRunAndAnswers504() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        AtomicReference<Throwable> holderFailure = new AtomicReference<>();
        Thread holder = new Thread(() -> {
            try (Connection connection = dataSource.getConnection()) {
                held.countDown();
                Thread.sleep(1_500);
            } catch (Exception e) {
                holderFailure.set(e);
            }
        });
        holder.start();
        held.await();

        MockMvcBuilders.standaloneSetup(new BfController(dataSource))
                .addInterceptors(new BFQueryTimeoutInterceptor(Duration.ofSeconds(1)))
                .setControllerAdvice(new BFTimeoutResponseAdvice(), new BFDataAccessExceptionHandler(),
                        new BFGlobalExceptionHandler())
                .build()
                .perform(get("/bf/insert"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("Gateway Timeout"))
                .andExpect(jsonPath("$.status_code").value(504));

        holder.join();
        assertThat(holderFailure.get()).isNull();
        assertThat(rows()).isZero();
    }
}
