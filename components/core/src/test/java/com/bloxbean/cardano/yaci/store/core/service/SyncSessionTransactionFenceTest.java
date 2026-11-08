package com.bloxbean.cardano.yaci.store.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real JDBC transactions (H2) with the fence registered on the transaction manager, as Spring Boot does.
 */
class SyncSessionTransactionFenceTest {
    private SyncSessionGuard guard;
    private JdbcTemplate jdbc;
    private DriverManagerDataSource dataSource;
    //Lets a test hold a commit in progress, between the session check and the end of the commit
    private volatile CountDownLatch inCommit;
    private volatile CountDownLatch releaseCommit;

    @BeforeEach
    void setUp() {
        guard = new SyncSessionGuard();
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:fence;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table if not exists cursor_probe (block bigint)");
        jdbc.execute("delete from cursor_probe");
        inCommit = null;
        releaseCommit = null;
        fence = null;
    }

    private SyncSessionTransactionFence fence;

    private TransactionTemplate transactions() {
        DataSourceTransactionManager tm = new DataSourceTransactionManager(dataSource) {
            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                CountDownLatch in = inCommit, release = releaseCommit;
                if (in != null) {
                    in.countDown();
                    await(release);
                }
                super.doCommit(status);
            }
        };
        if (fence == null)
            fence = new SyncSessionTransactionFence(guard);
        tm.setTransactionExecutionListeners(List.of(fence));
        return new TransactionTemplate(tm);
    }

    private long rows() {
        return jdbc.queryForObject("select count(*) from cursor_probe", Long.class);
    }

    @Test
    void aSessionChangeAfterTheLastCheckRollsTheCommitBack() {
        long session = guard.newSession();
        guard.enter(session);
        try {
            assertThatThrownBy(() -> transactions().executeWithoutResult(status -> {
                jdbc.update("insert into cursor_probe values (101)");
                guard.checkCaller(); //the last check in the code passes
                //A restart starts a new session after that check, before the commit
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        CompletableFuture.runAsync(guard::newSession).join();
                    }
                });
            })).isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        assertThat(rows()).isZero();
    }

    @Test
    void aSessionChangeWaitsForACommitInFlight() throws Exception {
        long session = guard.newSession();
        inCommit = new CountDownLatch(1);
        releaseCommit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var commit = CompletableFuture.runAsync(() -> {
                guard.enter(session);
                try {
                    transactions().executeWithoutResult(status -> jdbc.update("insert into cursor_probe values (101)"));
                } finally {
                    guard.exit();
                }
            }, pool);
            assertThat(inCommit.await(5, TimeUnit.SECONDS)).isTrue();

            var change = CompletableFuture.supplyAsync(guard::newSession, pool);
            Thread.sleep(300);
            assertThat(change).isNotDone(); //the commit passed its check, so the session waits for it

            releaseCommit.countDown();
            commit.get(5, TimeUnit.SECONDS);
            assertThat(change.get(5, TimeUnit.SECONDS)).isEqualTo(session + 1);
            assertThat(rows()).isEqualTo(1);
        } finally {
            releaseCommit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void aCommitThatHangsDoesNotHoldUpTheSessionChangeForever() throws Exception {
        guard.sessionChangeWaitMs = 200;
        long session = guard.newSession();
        inCommit = new CountDownLatch(1);
        releaseCommit = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture.runAsync(() -> {
                guard.enter(session);
                try {
                    transactions().executeWithoutResult(status -> jdbc.update("insert into cursor_probe values (101)"));
                } finally {
                    guard.exit();
                }
            }, pool);
            assertThat(inCommit.await(5, TimeUnit.SECONDS)).isTrue();

            long start = System.nanoTime();
            assertThat(CompletableFuture.supplyAsync(guard::newSession).get(5, TimeUnit.SECONDS)).isEqualTo(session + 1);
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(3_000);
        } finally {
            releaseCommit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void aStartWaitsForACommitThatOutlivedTheSessionChange() throws Exception {
        guard.sessionChangeWaitMs = 200;
        long session = guard.newSession();
        inCommit = new CountDownLatch(1);
        releaseCommit = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            var commit = CompletableFuture.runAsync(() -> {
                guard.enter(session);
                try {
                    transactions().executeWithoutResult(status -> jdbc.update("insert into cursor_probe values (101)"));
                } finally {
                    guard.exit();
                }
            }, pool);
            assertThat(inCommit.await(5, TimeUnit.SECONDS)).isTrue();

            //stop: the session changes after the bounded wait, with the commit still in flight
            CompletableFuture.runAsync(guard::endSession).get(5, TimeUnit.SECONDS);
            //start: refuses to go on (and is retried later) while that commit can still land
            assertThatThrownBy(() -> CompletableFuture.runAsync(guard::awaitCommitsOfOldSessions).join())
                    .hasCauseInstanceOf(IllegalStateException.class);

            releaseCommit.countDown();
            commit.get(5, TimeUnit.SECONDS);
            CompletableFuture.runAsync(guard::awaitCommitsOfOldSessions).get(5, TimeUnit.SECONDS);
        } finally {
            releaseCommit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void aCommitThatFailsBeforeTheFenceLeavesNothingBehind() {
        long session = guard.newSession();
        guard.enter(session);
        try {
            assertThatThrownBy(() -> transactions().executeWithoutResult(status -> {
                jdbc.update("insert into cursor_probe values (101)");
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        throw new org.springframework.transaction.TransactionTimedOutException("timed out");
                    }
                });
            })).isInstanceOf(org.springframework.transaction.TransactionTimedOutException.class);
        } finally {
            guard.exit();
        }

        assertThat(fence.tracked()).isZero();
        assertThat(CompletableFuture.supplyAsync(guard::newSession).join()).isEqualTo(session + 1);
    }

    @Test
    void aTransactionOutsideAnySessionIsNotFenced() {
        guard.newSession();

        transactions().executeWithoutResult(status -> {
            jdbc.update("insert into cursor_probe values (101)");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    CompletableFuture.runAsync(guard::newSession).join();
                }
            });
        });

        assertThat(rows()).isEqualTo(1);
    }

    @Test
    void aWorkerTransactionOfAnOldSessionIsRolledBack() throws Exception {
        long session = guard.newSession();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        AtomicReference<Long> workerSessionAfterTask = new AtomicReference<>(-1L);
        try {
            guard.enter(session);
            var executor = guard.propagating(pool);
            guard.exit();

            var task = CompletableFuture.runAsync(() -> transactions().executeWithoutResult(status -> {
                jdbc.update("insert into cursor_probe values (101)");
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        CompletableFuture.runAsync(guard::newSession).join();
                    }
                });
            }), executor);

            assertThatThrownBy(() -> task.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
            //the pooled thread does not keep the session after the task
            pool.submit(() -> workerSessionAfterTask.set(guard.callerSession())).get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(rows()).isZero();
        assertThat(workerSessionAfterTask.get()).isNull();
    }

    @Test
    void theCurrentSessionCommits() {
        long session = guard.newSession();
        guard.enter(session);
        try {
            transactions().executeWithoutResult(status -> jdbc.update("insert into cursor_probe values (101)"));
            transactions().executeWithoutResult(status -> jdbc.update("insert into cursor_probe values (102)"));
        } finally {
            guard.exit();
        }

        assertThat(rows()).isEqualTo(2);
        assertThat(fence.tracked()).isZero();
        //nothing is left holding the commit lock
        assertThat(CompletableFuture.supplyAsync(guard::newSession).join()).isEqualTo(session + 1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
