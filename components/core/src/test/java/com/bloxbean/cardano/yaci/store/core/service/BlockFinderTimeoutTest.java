package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * BlockFinder against a node that answers, refuses, never answers, or answers too late. No network.
 */
class BlockFinderTimeoutTest {
    private final Point point = new Point(100, "aa");

    @Test
    void foundAndNotFoundAreAnswered() {
        BlockSync found = mock(BlockSync.class);
        doAnswer(inv -> {
            inv.<BlockChainDataListener>getArgument(1).intersactFound(new Tip(point, 1), point);
            return null;
        }).when(found).startSync(any(Point.class), any(BlockChainDataListener.class));
        assertThat(new BlockFinder(found).blockExists(point, 5000)).isTrue();
        verify(found, atLeastOnce()).stop();

        BlockSync notFound = mock(BlockSync.class);
        doAnswer(inv -> {
            inv.<BlockChainDataListener>getArgument(1).intersactNotFound(new Tip(point, 1));
            return null;
        }).when(notFound).startSync(any(Point.class), any(BlockChainDataListener.class));
        assertThat(new BlockFinder(notFound).blockExists(point, 5000)).isFalse();
    }

    @Test
    void aNodeThatNeverAnswersIsAnErrorNotAMissingBlock() throws Exception {
        // Like yaci's handshake loop: sleep until interrupted
        CountDownLatch interrupted = new CountDownLatch(1);
        BlockSync silent = mock(BlockSync.class);
        doAnswer(inv -> {
            try {
                while (true) Thread.sleep(100);
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw new RuntimeException(e);
            }
        }).when(silent).startSync(any(Point.class), any(BlockChainDataListener.class));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> new BlockFinder(silent).blockExists(point, 300))
                .isInstanceOf(NodeNotAnsweringException.class);
        assertThat(System.currentTimeMillis() - start).isLessThan(5000);

        // The waiting thread was released and the connection closed
        assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
        verify(silent, atLeastOnce()).stop();
    }

    @Test
    void aStartThatSwallowsTheInterruptAndReturnsWithoutAnAnswerIsReleased() throws Exception {
        // yaci's NodeClient.start catches and logs the handshake failure, so startSync returns normally
        CountDownLatch workerDone = new CountDownLatch(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        BlockSync silent = mock(BlockSync.class);
        doAnswer(inv -> {
            worker.set(Thread.currentThread());
            try {
                while (true) Thread.sleep(100);
            } catch (InterruptedException e) {
                return null;
            }
        }).when(silent).startSync(any(Point.class), any(BlockChainDataListener.class));

        assertThatThrownBy(() -> new BlockFinder(silent).blockExists(point, 300))
                .isInstanceOf(NodeNotAnsweringException.class);

        worker.get().join(5000);
        assertThat(worker.get().isAlive()).isFalse();
    }

    @Test
    void anAnswerAfterTheTimeoutIsIgnored() {
        AtomicReference<BlockChainDataListener> listener = new AtomicReference<>();
        BlockSync slow = mock(BlockSync.class);
        doAnswer(inv -> {
            listener.set(inv.getArgument(1));
            return null; // connected, but the answer has not come yet
        }).when(slow).startSync(any(Point.class), any(BlockChainDataListener.class));

        assertThatThrownBy(() -> new BlockFinder(slow).blockExists(point, 300))
                .isInstanceOf(NodeNotAnsweringException.class);

        // The late answer must not throw or change anything
        listener.get().intersactFound(new Tip(point, 1), point);
    }

    @Test
    void withNoLimitOnTheConnectionTheAnswerIsStillWaitedForOnlySoLong() {
        // auto-restart off: no limit on the connection, but a node that connects and never answers must not
        // hold the startup forever
        BlockSync silent = mock(BlockSync.class); // connects, then never answers
        BlockFinder blockFinder = new BlockFinder(silent);
        blockFinder.answerTimeoutMs = 300;

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatThrownBy(() -> blockFinder.blockExists(point, 0))
                        .isInstanceOf(NodeNotAnsweringException.class));
        verify(silent, atLeastOnce()).stop();
    }

    @Test
    void aFailedConnectionIsAnError() {
        BlockSync broken = mock(BlockSync.class);
        doThrow(new IllegalStateException("connection refused")).when(broken).startSync(any(Point.class), any(BlockChainDataListener.class));

        assertThatThrownBy(() -> new BlockFinder(broken).blockExists(point, 5000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("connection refused");
    }

    @Test
    void anInterruptedCallerKeepsItsInterruptAndTheWorkerIsCleanedUp() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        BlockSync silent = mock(BlockSync.class);
        doAnswer(inv -> {
            worker.set(Thread.currentThread());
            while (true) Thread.sleep(100);
        }).when(silent).startSync(any(Point.class), any(BlockChainDataListener.class));

        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicReference<Boolean> stillInterrupted = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                new BlockFinder(silent).blockExists(point, 0);
            } catch (Throwable t) {
                thrown.set(t);
            }
            stillInterrupted.set(Thread.currentThread().isInterrupted());
        });
        caller.start();
        while (worker.get() == null) Thread.sleep(10);
        caller.interrupt();
        caller.join(10_000);

        assertThat(thrown.get()).isInstanceOf(IllegalStateException.class);
        assertThat(stillInterrupted.get()).isTrue();
        worker.get().join(5000);
        assertThat(worker.get().isAlive()).isFalse();
    }
}
