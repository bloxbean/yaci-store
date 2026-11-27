package com.bloxbean.cardano.yaci.store.core.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DatabaseStartupWaitTest {

    //fake time: only moves when the code under test sleeps (or a test moves it)
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final List<Long> sleeps = new ArrayList<>();

    private DatabaseStartupWait fakeTimeWait(String seconds) {
        DatabaseStartupWait wait = new DatabaseStartupWait(now::get, ms -> {
            sleeps.add(ms);
            now.addAndGet(ms);
        });
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, seconds));
        return wait;
    }

    private static DataSource refusing() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));
        return dataSource;
    }

    @Test
    void waitsUntilTheDatabaseAnswers() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("the database system is starting up"))
                .thenReturn(mock(Connection.class));
        DatabaseStartupWait wait = fakeTimeWait("30");

        wait.postProcessAfterInitialization(dataSource, "dataSource");

        verify(dataSource, times(2)).getConnection();
        assertThat(sleeps).containsExactly(DatabaseStartupWait.RETRY_INTERVAL_MS);
    }

    @Test
    void shortBudgetIsNotOverrun() throws Exception {
        DataSource dataSource = refusing();
        long start = now.get();

        fakeTimeWait("1").postProcessAfterInitialization(dataSource, "dataSource");

        //sleeps only the remaining 1 s, tries once more at the deadline, then gives up
        assertThat(sleeps).containsExactly(1_000L);
        assertThat(now.get() - start).isLessThanOrEqualTo(1_000);
        verify(dataSource, times(2)).getConnection();
    }

    @Test
    void lastPauseIsCappedToTheRemainingBudget() throws Exception {
        DataSource dataSource = refusing();

        fakeTimeWait("12").postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(sleeps).containsExactly(5_000L, 5_000L, 2_000L);
        verify(dataSource, times(4)).getConnection();
    }

    @Test
    void databaseThatAnswersAtTheDeadlineIsUsed() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("Connection refused"))
                .thenReturn(mock(Connection.class));

        fakeTimeWait("1").postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(sleeps).containsExactly(1_000L);
        verify(dataSource, times(2)).getConnection();
    }

    @Test
    void noAttemptAfterAPauseThatEndsLate() throws Exception {
        DataSource dataSource = refusing();
        //the thread wakes up 1 s after the pause should have ended, past the deadline
        DatabaseStartupWait wait = new DatabaseStartupWait(now::get, ms -> {
            sleeps.add(ms);
            now.addAndGet(ms + 1_000);
        });
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "1"));

        wait.postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(sleeps).containsExactly(1_000L);
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    void noPauseWhenAnAttemptUsesUpTheBudget() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenAnswer(inv -> {
            now.addAndGet(2_000); //a slow refusal, longer than the budget
            throw new SQLException("Connection timed out");
        });

        fakeTimeWait("1").postProcessAfterInitialization(dataSource, "dataSource");

        assertThat(sleeps).isEmpty();
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    void shortBudgetIsNotOverrunWithTheRealClock() throws Exception {
        DataSource dataSource = refusing();
        DatabaseStartupWait wait = new DatabaseStartupWait();
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "1"));

        long start = System.nanoTime();
        wait.postProcessAfterInitialization(dataSource, "dataSource");
        long tookMs = (System.nanoTime() - start) / 1_000_000;

        //1 s budget; the old code always slept 5 s before its second attempt. Wide margin for slow CI machines.
        assertThat(tookMs).isLessThan(3_500);
        verify(dataSource, atLeast(1)).getConnection();
        verify(dataSource, atMost(2)).getConnection();
    }

    @Test
    void stopsWhenInterrupted() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        CountDownLatch firstAttempt = new CountDownLatch(1);
        when(dataSource.getConnection()).thenAnswer(inv -> {
            firstAttempt.countDown();
            throw new SQLException("Connection refused");
        });
        DatabaseStartupWait wait = new DatabaseStartupWait();
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "300"));
        AtomicBoolean interruptFlagKept = new AtomicBoolean();

        Thread t = new Thread(() -> {
            wait.postProcessAfterInitialization(dataSource, "dataSource");
            interruptFlagKept.set(Thread.currentThread().isInterrupted());
        });
        t.start();
        assertThat(firstAttempt.await(10, TimeUnit.SECONDS)).isTrue();
        t.interrupt();
        t.join(10_000);

        assertThat(t.isAlive()).isFalse();
        assertThat(interruptFlagKept).isTrue();
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    void zeroTurnsItOff() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        DatabaseStartupWait wait = new DatabaseStartupWait();
        wait.setEnvironment(new MockEnvironment().withProperty(DatabaseStartupWait.WAIT_SECONDS_PROPERTY, "0"));

        wait.postProcessAfterInitialization(dataSource, "dataSource");

        verify(dataSource, never()).getConnection();
    }

    @Test
    void otherBeansAreLeftAlone() {
        DatabaseStartupWait wait = new DatabaseStartupWait();
        Object bean = new Object();

        assertThat(wait.postProcessAfterInitialization(bean, "x")).isSameAs(bean);
    }
}
