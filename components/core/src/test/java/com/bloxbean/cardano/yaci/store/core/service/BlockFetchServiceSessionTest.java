package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.exception.BlockParseRuntimeException;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Only the current sync session may write (#1129, #1328 findings 2 and 4).
 */
class BlockFetchServiceSessionTest {
    private static final String START_HASH = "h100";

    private BlockSync blockSync;
    private BlockRangeSync blockRangeSync;
    private StoreProperties storeProperties;
    private CursorService cursorService;
    private ShelleyBlockEventPublisher shelleyPublisher;
    private ErrorStorage errorStorage;
    private SyncSessionGuard guard;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        blockSync = mock(BlockSync.class);
        blockRangeSync = mock(BlockRangeSync.class);
        cursorService = mock(CursorService.class);
        shelleyPublisher = mock(ShelleyBlockEventPublisher.class);
        EraService eraService = mock(EraService.class);
        storeProperties = mock(StoreProperties.class);
        errorStorage = mock(ErrorStorage.class);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);

        blockFetchService = new BlockFetchService(
                mock(ApplicationEventPublisher.class),
                mock(MetricsService.class),
                blockRangeSync,
                blockSync,
                cursorService,
                eraService,
                storeProperties,
                mock(GenesisConfig.class),
                shelleyPublisher,
                mock(ByronBlockEventPublisher.class),
                errorStorage);
        guard = new SyncSessionGuard();
        blockFetchService.setSessionGuard(guard);

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @AfterEach
    void tearDown() {
        blockFetchService.stop();
    }

    @Test
    void theRestOfAStartRunsAsTheNewSession() {
        //batchDone at the end of the range fetch starts chain sync on a thread of the range session. That session
        //becomes stale as chain sync opens its own, so the start's own reads (the cursor) must run as the new one,
        //or the commit fence would roll them back.
        long rangeSession = guard.newSession();
        var sessionDuringCursorRead = new java.util.concurrent.atomic.AtomicReference<Long>();
        when(cursorService.getCursorByBlockHash("aa")).thenAnswer(inv -> {
            sessionDuringCursorRead.set(guard.callerSession());
            return java.util.Optional.empty();
        });

        guard.enter(rangeSession);
        try {
            blockFetchService.startSync(new Point(100, "aa"), 10L);

            assertThat(sessionDuringCursorRead.get()).isEqualTo(guard.currentSession());
            assertThat(guard.callerSession()).isEqualTo(rangeSession); //restored for the rest of batchDone
        } finally {
            guard.exit();
        }
    }

    @Test
    void chainSyncDoesNotStartWhileACommitOfTheOldSessionIsRunning() throws Exception {
        //e.g. batchDone after a parallel worker timed out: the error is set, and a worker of the old range session
        //has passed the commit fence and is still committing
        guard.sessionChangeWaitMs = 100;
        long old = guard.newSession();
        ((java.util.concurrent.atomic.AtomicBoolean) org.springframework.test.util.ReflectionTestUtils
                .getField(blockFetchService, "isError")).set(true);
        var inCommit = new java.util.concurrent.CountDownLatch(1);
        var done = new java.util.concurrent.CountDownLatch(1);
        Thread committer = new Thread(() -> {
            Runnable release = guard.enterCommit(old);
            inCommit.countDown();
            try {
                done.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                release.run();
            }
        });
        committer.start();
        try {
            org.assertj.core.api.Assertions.assertThat(inCommit.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> blockFetchService.startSync(new Point(100, "aa"), null))
                    .isInstanceOf(IllegalStateException.class);

            //the error is kept, so the restart is tried again, and nothing was started
            org.assertj.core.api.Assertions.assertThat(blockFetchService.isError()).isTrue();
            verify(blockSync, never()).startSync(any(), any());
        } finally {
            done.countDown();
            committer.join(5_000);
        }
    }

    @Test
    void blockFromAnOldSessionIsIgnored() {
        BlockChainDataListener oldSession = startSyncAndGetListener();
        BlockChainDataListener newSession = startSyncAndGetListener();

        Block lateBlock = block(101, "h101", START_HASH);
        oldSession.onBlock(Era.Conway, lateBlock, List.of());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(lateBlock), any());

        Block block = block(101, "h101", START_HASH);
        newSession.onBlock(Era.Conway, block, List.of());
        verify(shelleyPublisher).publishBlockEvents(any(), eq(block), any());
    }

    @Test
    void rollbackFromAStoppedSessionIsIgnored() {
        BlockChainDataListener session = startSyncAndGetListener();
        blockFetchService.stop();

        session.onRollback(new Point(90, "h90"));

        verify(cursorService, never()).rollback(anyLong());
    }

    @Test
    void lateWriteFromAnOldSessionDoesNotStopTheCurrentOne() {
        //The old session's thread was inside the block when a restart started a new session. The publisher refuses
        //to commit; that must not be treated as a failure of the current session.
        BlockChainDataListener session = startSyncAndGetListener();
        doAnswer(inv -> {
            guard.newSession(); //a restart happens while this block is being written
            guard.checkCaller();
            return null;
        }).when(shelleyPublisher).publishBlockEvents(any(), any(), any());

        assertThatThrownBy(() -> session.onBlock(Era.Conway, block(101, "h101", START_HASH), List.of()))
                .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);

        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void anyErrorInAnOldSessionsThreadDoesNotStopTheCurrentOne() {
        //e.g. the shared batch list was reset by the new session while the old thread waited
        BlockChainDataListener session = startSyncAndGetListener();
        doAnswer(inv -> {
            guard.newSession();
            throw new java.util.NoSuchElementException("batch emptied by the new session");
        }).when(shelleyPublisher).publishBlockEvents(any(), any(), any());

        assertThatThrownBy(() -> session.onBlock(Era.Conway, block(101, "h101", START_HASH), List.of()))
                .isInstanceOf(java.util.NoSuchElementException.class);

        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void oldSessionCannotStartAnotherSession() {
        //e.g. an old range session that finishes a long batchDone after a restart already started a new session
        startSyncAndGetListener();
        long oldSession = guard.currentSession();
        guard.newSession();

        guard.enter(oldSession);
        try {
            blockFetchService.startSync(new Point(100, START_HASH), 100L);
        } finally {
            guard.exit();
        }

        verify(blockSync, times(1)).startSync(any(), any());
    }

    @Test
    void lateBatchDoneDoesNotStopTheSessionThatReplacedIt() throws Exception {
        //#1344 finding 3: the old range session's batchDone passes the session check, then waits for the lock while a
        //restart starts a new range fetch. It must not stop the new fetch.
        when(storeProperties.isPrimaryInstance()).thenReturn(true);
        when(cursorService.getCursor()).thenReturn(Optional.of(
                Cursor.builder().slot(100L).block(100L).blockHash(START_HASH).prevBlockHash("h99").era(Era.Conway).build()));
        blockFetchService.startFetch(new Point(100, START_HASH), new Point(200, "h200"), 100L);
        var captor = ArgumentCaptor.forClass(BlockChainDataListener.class);
        verify(blockRangeSync).restart(captor.capture());
        BlockChainDataListener oldSession = captor.getValue();

        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread lateBatchDone;
        synchronized (blockFetchService) {
            lateBatchDone = new Thread(() -> {
                try {
                    oldSession.batchDone();
                } catch (Throwable e) {
                    error.set(e);
                }
            });
            lateBatchDone.start();
            long deadline = System.currentTimeMillis() + 10_000;
            while (lateBatchDone.getState() != Thread.State.BLOCKED) {
                if (System.currentTimeMillis() > deadline)
                    throw new AssertionError("batchDone did not wait for the lock: " + lateBatchDone.getState());
                Thread.sleep(5);
            }

            //The restart
            blockFetchService.startFetch(new Point(100, START_HASH), new Point(200, "h200"), 100L);
            clearInvocations(blockRangeSync, blockSync);
        }
        lateBatchDone.join(10_000);
        assertThat(lateBatchDone.isAlive()).isFalse();

        assertThat(error.get()).isNull();
        verify(blockRangeSync, never()).stop();
        verify(blockSync, never()).startSync(any(), any());
    }

    @Test
    void anOldThreadResumingAfterARestartDoesNotMarkItsBlockAccepted() throws Exception {
        //#1332 review finding 1: the old range thread queues block 101 and waits before recording it. A restart from
        //the cursor (block 100) resets the state. The old thread must not then record 101 as accepted, or the new
        //session would skip 101 as a re-delivery: it is not written (the old batch is dropped).
        when(storeProperties.isEnableParallelProcessing()).thenReturn(true);
        BlockChainDataListener oldSession = startFetchFromCursorAndGetListener();
        var restart = new java.util.concurrent.atomic.AtomicReference<BlockChainDataListener>();
        doAnswer(inv -> {
            restart.set(onAnotherThread(this::startFetchFromCursorAndGetListener));
            return null;
        }).when(shelleyPublisher).publishBlockEventsInParallel(any(), any(), any());

        assertThatThrownBy(() -> oldSession.onBlock(Era.Conway, block(101, "h101", START_HASH), List.of()))
                .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);

        doNothing().when(shelleyPublisher).publishBlockEventsInParallel(any(), any(), any());
        Block replayed = block(101, "h101", START_HASH);
        restart.get().onBlock(Era.Conway, replayed, List.of());

        verify(shelleyPublisher).publishBlockEventsInParallel(any(), eq(replayed), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void aHangingFinalBatchWriteDoesNotBlockARestart() throws Exception {
        //#1332 review finding 2: the last batch of the range fetch is written while a restart stops the old session
        //and starts a new one. The restart must not wait for the write, and the old session must not then start chain
        //sync.
        when(storeProperties.isPrimaryInstance()).thenReturn(true);
        BlockChainDataListener oldSession = startFetchFromCursorAndGetListener();
        var inWrite = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(inv -> {
            inWrite.countDown();
            release.await(10, java.util.concurrent.TimeUnit.SECONDS);
            return null;
        }).when(shelleyPublisher).processBlocksInParallel();
        Thread batchDone = new Thread(oldSession::batchDone);
        batchDone.start();
        try {
            assertThat(inWrite.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

            //The restart, on this thread: it would block here if batchDone held the service lock during the write
            var restarted = java.util.concurrent.CompletableFuture.runAsync(() -> {
                blockFetchService.stop();
                startFetchFromCursorAndGetListener();
            });
            restarted.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            release.countDown();
            batchDone.join(10_000);
        }

        assertThat(batchDone.isAlive()).isFalse();
        verify(blockSync, never()).startSync(any(), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void aStopDuringTheFinalBatchWriteKeepsChainSyncFromStarting() throws Exception {
        //#1332 review finding 5: nothing is running between closing the range connection and starting chain sync. A
        //stop in that gap must still keep the range session from starting chain sync.
        when(storeProperties.isPrimaryInstance()).thenReturn(true);
        BlockChainDataListener session = startFetchFromCursorAndGetListener();
        doAnswer(inv -> {
            onAnotherThread(() -> {
                blockFetchService.stop();
                return null;
            });
            return null;
        }).when(shelleyPublisher).processBlocksInParallel();

        session.batchDone();

        verify(blockSync, never()).startSync(any(), any());
        assertThat(blockFetchService.isScheduledToStop()).isTrue();
        //The stop went through while the batch was written, and the write did not fail the sync
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void aParseErrorFromAnOldSessionDoesNotStopTheReplacement() throws Exception {
        //#1332 review finding 3: a restart happens while the parse error row is saved, so the commit fence refuses it.
        //The old thread must not flag the new session as failed or close its connection.
        when(storeProperties.isContinueOnParseError()).thenReturn(true);
        BlockChainDataListener oldSession = startFetchFromCursorAndGetListener();
        doAnswer(inv -> {
            onAnotherThread(this::startFetchFromCursorAndGetListener);
            clearInvocations(blockRangeSync, blockSync);
            guard.checkCaller();
            return null;
        }).when(errorStorage).save(any());

        assertThatThrownBy(() -> oldSession.onParsingError(
                new BlockParseRuntimeException(101L, null, new RuntimeException("bad cbor"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(blockFetchService.isError()).isFalse();
        verify(blockRangeSync, never()).stop();
        verify(blockSync, never()).stop();
    }

    @Test
    void aFailedStartAfterAStopLeavesTheSyncFailedNotStopped() {
        //#1332 review finding 4: a restart stops the sync, then its start fails (a commit of the old session is still
        //running). The health check skips a sync that is scheduled to stop, so it must look failed instead.
        startSyncAndGetListener();
        blockFetchService.stop();
        assertThat(blockFetchService.isScheduledToStop()).isTrue();

        blockFetchService.markStartFailed();

        assertThat(blockFetchService.isScheduledToStop()).isFalse();
        assertThat(blockFetchService.isError()).isTrue();
    }

    private BlockChainDataListener startSyncAndGetListener() {
        clearInvocations(blockSync);
        when(cursorService.getCursor()).thenReturn(Optional.of(
                Cursor.builder().slot(100L).block(100L).blockHash(START_HASH).prevBlockHash("h99").era(Era.Conway).build()));
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        var captor = ArgumentCaptor.forClass(BlockChainDataListener.class);
        verify(blockSync).startSync(any(), captor.capture());
        return captor.getValue();
    }

    private BlockChainDataListener startFetchFromCursorAndGetListener() {
        clearInvocations(blockRangeSync);
        Cursor cursor = Cursor.builder().slot(100L).block(100L).blockHash(START_HASH).prevBlockHash("h99")
                .era(Era.Conway).build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(cursorService.getCursorByBlockHash(START_HASH)).thenReturn(Optional.of(cursor));
        blockFetchService.startFetch(new Point(100, START_HASH), new Point(200, "h200"), 100L);
        var captor = ArgumentCaptor.forClass(BlockChainDataListener.class);
        verify(blockRangeSync).restart(captor.capture());
        return captor.getValue();
    }

    //Runs on a thread outside any sync session, like a restart, and waits for it
    private static <T> T onAnotherThread(java.util.concurrent.Callable<T> task) throws Exception {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private Block block(long number, String hash, String prevHash) {
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        when(block.getHeader().getHeaderBody().getBlockNumber()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getSlot()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getBlockHash()).thenReturn(hash);
        when(block.getHeader().getHeaderBody().getPrevHash()).thenReturn(prevHash);
        when(block.getHeader().getHeaderBody().getIssuerVkey()).thenReturn("a".repeat(64));
        return block;
    }
}
