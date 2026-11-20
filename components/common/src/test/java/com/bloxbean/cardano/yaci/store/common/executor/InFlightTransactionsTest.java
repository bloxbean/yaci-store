package com.bloxbean.cardano.yaci.store.common.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InFlightTransactionsTest {
    private static final Duration GUARD = Duration.ofSeconds(20);

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private TransactionTemplate requiresNew;
    private final CountDownLatch release = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        // A short lock timeout, so a row lock that is still held fails the check below quickly
        dataSource = abortClosesConnection(new DriverManagerDataSource(
                "jdbc:h2:mem:inflight-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=500", "sa", ""));
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table balance (address varchar(64) primary key, quantity bigint)");

        requiresNew = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterEach
    void tearDown() {
        release.countDown();
    }

    @Test
    void timeout_abortsTheStuckTransaction_soItsRowLocksAreReleased() {
        InFlightTransactions inFlight = new InFlightTransactions(dataSource);
        CountDownLatch inserted = new CountDownLatch(1);

        // The worker writes a row and then never finishes, like the stake balance save stuck inside the driver
        CompletableFuture<Void> worker = CompletableFuture.runAsync(() -> requiresNew.execute(status -> {
            inFlight.trackCurrentTransaction();
            jdbc.update("insert into balance values ('stake1', 10)");
            inserted.countDown();
            awaitQuietly(release);
            return null;
        }), virtualThreads());

        assertTimeoutPreemptively(GUARD, () -> {
            assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(inFlight.trackedCount()).isEqualTo(1);

            assertThatThrownBy(() -> inFlight.awaitAll(Duration.ofMillis(300), "stake balance save", worker))
                    .isInstanceOf(DbWorkTimeoutException.class)
                    .hasMessageContaining("stake balance save")
                    .hasMessageContaining("Aborted 1 database connection");
        });

        // The aborted transaction rolled back: its row is gone and its lock no longer blocks the same key
        jdbc.update("insert into balance values ('stake1', 20)");
        assertThat(jdbc.queryForObject("select quantity from balance where address = 'stake1'", Long.class))
                .isEqualTo(20L);
        assertThat(inFlight.trackedCount()).isZero();
    }

    @Test
    void workerThatStartsAfterTheWaitGaveUp_isAbortedAndRolledBack() {
        InFlightTransactions inFlight = new InFlightTransactions(dataSource);
        CompletableFuture<Void> neverDone = new CompletableFuture<>();

        assertThatThrownBy(() -> inFlight.awaitAll(Duration.ofMillis(50), "late work", neverDone))
                .isInstanceOf(DbWorkTimeoutException.class);

        assertThatThrownBy(() -> requiresNew.execute(status -> {
            jdbc.update("insert into balance values ('stake2', 10)");
            inFlight.trackCurrentTransaction();
            return null;
        })).isInstanceOf(DbWorkTimeoutException.class);

        assertThat(jdbc.queryForObject("select count(*) from balance", Long.class)).isZero();
    }

    @Test
    void finishedWork_isNotAborted_andIsNoLongerTracked() {
        InFlightTransactions inFlight = new InFlightTransactions(dataSource);

        CompletableFuture<Void> worker = CompletableFuture.runAsync(() -> requiresNew.execute(status -> {
            inFlight.trackCurrentTransaction();
            jdbc.update("insert into balance values ('stake3', 30)");
            return null;
        }), virtualThreads());

        assertTimeoutPreemptively(GUARD, () -> inFlight.awaitAll(Duration.ofSeconds(10), "quick work", worker));

        assertThat(inFlight.trackedCount()).isZero();
        assertThat(jdbc.queryForObject("select quantity from balance where address = 'stake3'", Long.class))
                .isEqualTo(30L);
    }

    @Test
    void timeout_abortsTrackedConnections() throws Exception {
        InFlightTransactions inFlight = new InFlightTransactions(null);
        Connection stuck = mock(Connection.class);
        inFlight.track(stuck);

        assertTimeoutPreemptively(GUARD, () ->
                assertThatThrownBy(() -> inFlight.awaitAll(Duration.ofMillis(100), "work", new CompletableFuture<>()))
                        .isInstanceOf(DbWorkTimeoutException.class));

        verify(stuck).abort(any(Executor.class));
    }

    @Test
    void workerFailure_isRethrown_withoutAborting() throws Exception {
        InFlightTransactions inFlight = new InFlightTransactions(null);
        Connection connection = mock(Connection.class);
        inFlight.track(connection);
        IllegalStateException failure = new IllegalStateException("insert failed");

        assertThatThrownBy(() -> inFlight.awaitAll(Duration.ofSeconds(10), "work",
                CompletableFuture.completedFuture(null), CompletableFuture.failedFuture(failure)))
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);

        verify(connection, never()).abort(any(Executor.class));
    }

    @Test
    void noTimeout_waitsUntilDone() {
        InFlightTransactions inFlight = new InFlightTransactions(null);
        CompletableFuture<Void> later = CompletableFuture.runAsync(() -> sleepQuietly(200));

        assertTimeoutPreemptively(GUARD, () -> inFlight.awaitAll(Duration.ZERO, "work", later));
        assertThat(later).isDone();
    }

    /**
     * H2 does not implement {@link Connection#abort}. PostgreSQL's driver closes the socket there, and the server then
     * rolls the transaction back; closing the H2 connection has the same effect on the data, so the test uses that.
     */
    private static DataSource abortClosesConnection(DataSource target) {
        return new DelegatingDataSource(target) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = target.getConnection();
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("abort")) {
                                connection.close();
                                return null;
                            }
                            try {
                                return method.invoke(connection, args);
                            } catch (InvocationTargetException e) {
                                throw e.getTargetException();
                            }
                        });
            }
        };
    }

    private static Executor virtualThreads() {
        return runnable -> Thread.ofVirtual().start(runnable);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
