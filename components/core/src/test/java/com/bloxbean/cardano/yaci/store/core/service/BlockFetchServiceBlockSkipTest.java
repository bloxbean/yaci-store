package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.exception.BlockParseRuntimeException;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Paths that could skip a block without leaving any trace (issue #1308). Blocks are delivered the way
 * yaci 0.4.7's BlockfetchAgent does it: an exception thrown by onBlock is reported back as a parse error.
 */
class BlockFetchServiceBlockSkipTest {
    private static final String ISSUER_VKEY = "a".repeat(64);
    private static final String START_HASH = "h100";

    private ApplicationEventPublisher publisher;
    private BlockSync blockSync;
    private CursorService cursorService;
    private EraService eraService;
    private StoreProperties storeProperties;
    private ShelleyBlockEventPublisher shelleyPublisher;
    private ErrorStorage errorStorage;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        blockSync = mock(BlockSync.class);
        cursorService = mock(CursorService.class);
        eraService = mock(EraService.class);
        storeProperties = mock(StoreProperties.class);
        shelleyPublisher = mock(ShelleyBlockEventPublisher.class);
        errorStorage = mock(ErrorStorage.class);

        when(storeProperties.isEnableParallelProcessing()).thenReturn(true);
        when(storeProperties.isContinueOnParseError()).thenReturn(true);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);
        //The start block of most tests is a stored block in epoch 100
        when(cursorService.getCursorByBlockHash(START_HASH)).thenReturn(Optional.of(cursor(100, START_HASH)));

        blockFetchService = new BlockFetchService(
                publisher,
                mock(MetricsService.class),
                mock(BlockRangeSync.class),
                blockSync,
                cursorService,
                eraService,
                storeProperties,
                mock(GenesisConfig.class),
                shelleyPublisher,
                mock(ByronBlockEventPublisher.class),
                errorStorage);

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @AfterEach
    void tearDown() {
        blockFetchService.stop();
    }

    @Test
    void blockArrivingBeforeStartSyncReturnsIsWrittenNotLeftInABatch() {
        Block first = block(101, "h101", START_HASH);
        //BlockSync.startSync waits for the handshake. The node can deliver the first block during that wait.
        doAnswer(inv -> {
            deliver(first);
            return null;
        }).when(blockSync).startSync(any(), any());

        blockFetchService.startSync(new Point(100, START_HASH));

        verify(shelleyPublisher).publishBlockEvents(any(), eq(first), any());
        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), any(), any());
    }

    @Test
    void startSyncDropsBlocksLeftInABatchByRangeFetch() {
        blockFetchService.startSync(new Point(100, START_HASH));

        //They are fetched again from the cursor, so flushing them later would write them twice
        verify(shelleyPublisher).reset();
    }

    @Test
    void blockLeftInABatchDuringSyncIsCaughtByTheNextBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));

        //The original startup race: the first block is seen with syncMode still false and goes to a batch
        ReflectionTestUtils.setField(blockFetchService, "syncMode", false);
        deliver(block(101, "h101", START_HASH));
        ReflectionTestUtils.setField(blockFetchService, "syncMode", true);

        Block next = block(102, "h102", "h101");
        deliver(next);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void databaseFailureBeforeSavingStopsSyncInsteadOfSkippingTheBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));
        when(eraService.checkIfNewEra(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"))
                .thenReturn(false);

        Block lost = block(101, "h101", START_HASH);
        Block next = block(102, "h102", "h101");
        deliver(lost);
        deliver(next);

        assertThat(blockFetchService.isError()).isTrue();
        verify(blockSync, atLeastOnce()).stop();
        verify(shelleyPublisher, never()).publishBlockEvents(any(), any(), any());
        verify(errorStorage, never()).save(any());
        verify(publisher, never()).publishEvent(any(ErrorEvent.class));
    }

    @Test
    void blockThatDoesNotBuildOnThePreviousBlockIsNotWritten() {
        blockFetchService.startSync(new Point(100, START_HASH));

        Block linked = block(101, "h101", START_HASH);
        Block afterGap = block(103, "h103", "h102");
        deliver(linked);
        deliver(afterGap);

        verify(shelleyPublisher).publishBlockEvents(any(), eq(linked), any());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
        verify(publisher).publishEvent(any(RequiredSyncRestartEvent.class));
    }

    @Test
    void firstBlockAfterStartMustBuildOnTheStartPoint() {
        blockFetchService.startSync(new Point(100, START_HASH));

        Block afterGap = block(102, "h102", "h101");
        deliver(afterGap);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), any(), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void firstBlockAfterRollbackMustBuildOnTheRollbackPoint() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));
        deliver(block(102, "h102", "h101"));

        when(cursorService.getCursor()).thenReturn(Optional.of(cursor(102, "h102")), Optional.of(cursor(101, "h101")));
        blockFetchService.onRollback(new Point(101, "h101"));

        Block fork = block(102, "h102b", "h101");
        deliver(fork);

        verify(shelleyPublisher).publishBlockEvents(any(), eq(fork), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void rollbackToABlockThatWasNeverStoredIsNotTrusted() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));

        //yaci rolls back to a block it has but the store does not (cursor is still at 101)
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor(101, "h101")));
        blockFetchService.onRollback(new Point(102, "h102"));

        Block afterGap = block(103, "h103", "h102");
        deliver(afterGap);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorSkipsExactlyOneRecordedBlock() {
        blockFetchService.startSync(new Point(100, START_HASH));
        deliver(block(101, "h101", START_HASH));

        blockFetchService.onParsingError(new BlockParseRuntimeException(102L, null, new RuntimeException("bad cbor")));
        verify(errorStorage).save(any(ErrorEvent.class));

        //The skipped block's hash is unknown, so the next block is accepted without the link check
        Block afterSkip = block(103, "h103", "h102");
        deliver(afterSkip);
        verify(shelleyPublisher).publishBlockEvents(any(), eq(afterSkip), any());

        //The check resumes from there
        Block afterGap = block(105, "h105", "h104");
        deliver(afterGap);
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorAfterAMissingBlockStopsTheSync() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);

        //101 never arrives, then 102 fails to parse. Skipping 102 would let 103 through and hide 101.
        blockFetchService.onParsingError(new BlockParseRuntimeException(102L, null, new RuntimeException("bad cbor")));
        Block next = block(103, "h103", "h102");
        deliver(next);

        verify(errorStorage, never()).save(any());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isTrue();
        verify(publisher).publishEvent(any(RequiredSyncRestartEvent.class));
    }

    @Test
    void parseErrorAfterAMissingBlockInARangeBatchStopsTheSync() {
        blockFetchService.startFetch(new Point(100, START_HASH), TO, 100L);
        deliver(block(100, START_HASH, "h99"));
        deliver(block(101, "h101", START_HASH));

        //102 never arrives, then 103 fails to parse
        blockFetchService.onParsingError(new BlockParseRuntimeException(103L, null, new RuntimeException("bad cbor")));
        Block next = block(104, "h104", "h103");
        deliver(next);

        verify(errorStorage, never()).save(any());
        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorOfAByronEpochBoundaryBlockIsSkipped() {
        //The epoch boundary block has the same number as the main block before it
        blockFetchService.startSync(new Point(100, START_HASH), 100L);

        blockFetchService.onParsingError(new BlockParseRuntimeException(100L, null, new RuntimeException("bad cbor")));

        verify(errorStorage).save(any(ErrorEvent.class));
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void parseErrorThatCannotBeRecordedStopsSync() {
        blockFetchService.startSync(new Point(100, START_HASH));
        doThrow(new DataAccessResourceFailureException("connection refused")).when(errorStorage).save(any());

        assertThatThrownBy(() -> blockFetchService.onParsingError(
                new BlockParseRuntimeException(101L, null, new RuntimeException("bad cbor"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(blockFetchService.isError()).isTrue();
    }

    // --- Range fetch (initial sync, parallel batches) and re-delivered blocks

    private static final Point TO = new Point(10_000, "tip");

    @Test
    void rangeFetchSkipsTheCursorBlockItDeliversAgain() {
        //The range fetch starts at the cursor and delivers the cursor block itself again
        blockFetchService.startFetch(new Point(100, START_HASH), TO, 100L);

        Block cursorBlock = block(100, START_HASH, "h99");
        Block next = block(101, "h101", START_HASH);
        deliver(cursorBlock);
        deliver(next);

        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), eq(cursorBlock), any());
        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(cursorBlock), any());
        verify(shelleyPublisher).publishBlockEventsInParallel(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void freshStartProcessesTheFirstBlock() {
        //No cursor yet: the start point is the first block, which has never been processed
        blockFetchService.startFetch(new Point(0, "h0"), TO, null);

        Block first = block(0, "h0", "genesis");
        deliver(first);

        verify(shelleyPublisher).publishBlockEventsInParallel(any(), eq(first), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void rangeBatchWithAMissingBlockStopsTheSync() {
        blockFetchService.startFetch(new Point(100, START_HASH), TO, 100L);

        deliver(block(101, "h101", START_HASH));
        Block afterHole = block(103, "h103", "h102"); //102 never arrives
        deliver(afterHole);

        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), eq(afterHole), any());
        assertThat(blockFetchService.isError()).isTrue();
        verify(publisher).publishEvent(any(RequiredSyncRestartEvent.class));
    }

    @Test
    void blockDeliveredAgainInsideABatchIsSkippedOnce() {
        //e.g. a reconnect in the middle of a batch (#1319): blocks not yet written come again
        blockFetchService.startFetch(new Point(100, START_HASH), TO, 100L);

        Block b101 = block(101, "h101", START_HASH);
        Block b102 = block(102, "h102", "h101");
        deliver(b101);
        deliver(b102);
        deliver(block(101, "h101", START_HASH));
        deliver(block(102, "h102", "h101"));
        Block b103 = block(103, "h103", "h102");
        deliver(b103);

        verify(shelleyPublisher, times(1)).publishBlockEventsInParallel(any(), eq(b101), any());
        verify(shelleyPublisher, times(1)).publishBlockEventsInParallel(any(), eq(b102), any());
        verify(shelleyPublisher).publishBlockEventsInParallel(any(), eq(b103), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void chainSyncSkipsABlockThatIsAlreadyStored() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        when(cursorService.getCursorByBlockHash("h50")).thenReturn(Optional.of(cursor(50, "h50")));

        Block stored = block(50, "h50", "h49");
        deliver(stored);
        Block next = block(101, "h101", START_HASH);
        deliver(next);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(stored), any());
        verify(shelleyPublisher).publishBlockEvents(any(), eq(next), any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void unknownBlockBelowTheCursorStopsTheSync() {
        //A block from another fork, without a rollback first
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        when(cursorService.getCursorByBlockHash("x99")).thenReturn(Optional.empty());

        Block other = block(99, "x99", "h98");
        deliver(other);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(other), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void epochBoundaryBlockAfterASkippedParseErrorIsAccepted() {
        //Byron: the epoch boundary block has the same number as the main block before it
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        blockFetchService.onParsingError(new BlockParseRuntimeException(101L, null, new RuntimeException("bad cbor")));

        blockFetchService.onByronEbBlock(ebBlock(101, "eb101", "h101"));

        verify(publisher).publishEvent(any(com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent.class));
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void gapAfterASkippedParseErrorStopsTheSync() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        blockFetchService.onParsingError(new BlockParseRuntimeException(101L, null, new RuntimeException("bad cbor")));

        Block afterGap = block(103, "h103", "h102");
        deliver(afterGap);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(afterGap), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void epochBoundaryBlockReDeliveredIsSkipped() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        blockFetchService.onByronEbBlock(ebBlock(100, "eb100", START_HASH));
        blockFetchService.onByronEbBlock(ebBlock(100, "eb100", START_HASH));

        verify(publisher, times(1)).publishEvent(any(com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent.class));
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void placeholderCursorOfAFreshStartDoesNotCountAsProcessed() {
        //StartService saves the start point as cursor before the block is processed, with no previous hash
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        when(cursorService.getCursorByBlockHash("p50"))
                .thenReturn(Optional.of(Cursor.builder().slot(50L).block(50L).blockHash("p50").era(Era.Conway).build()));

        Block notProcessed = block(50, "p50", "h49");
        deliver(notProcessed);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(notProcessed), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void parseErrorWithoutABlockNumberIsSkippedWithoutFailing() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);

        blockFetchService.onParsingError(new BlockParseRuntimeException(null, null, new RuntimeException("bad header")));

        assertThat(blockFetchService.isError()).isFalse();
        Block next = block(102, "h102", "h101");
        deliver(next);
        verify(shelleyPublisher).publishBlockEvents(any(), eq(next), any());
    }

    @Test
    void rollbackThatLeavesNoCursorStillChecksTheNextBlock() {
        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        when(cursorService.getCursor())
                .thenReturn(Optional.of(cursor(100, START_HASH)))
                .thenReturn(Optional.empty());

        blockFetchService.onRollback(new Point(90, "h90"));
        Block notLinked = block(92, "h92", "h91");
        deliver(notLinked);

        verify(shelleyPublisher, never()).publishBlockEvents(any(), eq(notLinked), any());
        assertThat(blockFetchService.isError()).isTrue();
    }

    // --- Epoch state when a restart skips the start block

    @Test
    void restartToACursorInAnEarlierEpochSeesTheNextEpoch() {
        //The old session reached epoch 101. After a restart in the same process, the start block is the last stored
        //block, still in epoch 100. It is skipped as already processed, so it does not set the epoch.
        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 101);
        when(eraService.getEpochNo(Era.Conway, 100L)).thenReturn(100);
        when(eraService.getEpochNo(Era.Conway, 101L)).thenReturn(101);

        blockFetchService.startFetch(new Point(100, START_HASH), TO, 100L);
        deliver(block(100, START_HASH, "h99"));
        Block firstOfEpoch = block(101, "h101", START_HASH);
        deliver(firstOfEpoch);

        verify(publisher).publishEvent(argThat((Object ev) -> ev instanceof com.bloxbean.cardano.yaci.store.events.EpochChangeEvent epochChange
                && epochChange.getPreviousEpoch() == 100 && epochChange.getEpoch() == 101));
        //Written on its own after the epoch change, not left in a batch
        verify(shelleyPublisher).publishBlockEvents(any(), eq(firstOfEpoch), any());
        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), eq(firstOfEpoch), any());
    }

    @Test
    void chainSyncRestartToACursorInAnEarlierEpochSeesTheNextEpoch() {
        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 101);
        when(eraService.getEpochNo(Era.Conway, 100L)).thenReturn(100);
        when(eraService.getEpochNo(Era.Conway, 101L)).thenReturn(101);

        blockFetchService.startSync(new Point(100, START_HASH), 100L);
        deliver(block(101, "h101", START_HASH));

        verify(publisher).publishEvent(argThat((Object ev) -> ev instanceof com.bloxbean.cardano.yaci.store.events.EpochChangeEvent epochChange
                && epochChange.getPreviousEpoch() == 100 && epochChange.getEpoch() == 101));
    }

    private com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock ebBlock(long number, String hash, String prevHash) {
        var eb = mock(com.bloxbean.cardano.yaci.core.model.byron.ByronEbBlock.class, RETURNS_DEEP_STUBS);
        when(eb.getHeader().getConsensusData().getDifficulty()).thenReturn(java.math.BigInteger.valueOf(number));
        when(eb.getHeader().getConsensusData().getEpoch()).thenReturn(5L);
        when(eb.getHeader().getBlockHash()).thenReturn(hash);
        when(eb.getHeader().getPrevBlock()).thenReturn(prevHash);
        return eb;
    }

    //Same dispatch as yaci 0.4.7 BlockfetchAgent: an exception from the listener becomes onParsingError
    private void deliver(Block block) {
        try {
            blockFetchService.onBlock(Era.Conway, block, List.of());
        } catch (Exception e) {
            blockFetchService.onParsingError(
                    new BlockParseRuntimeException(block.getHeader().getHeaderBody().getBlockNumber(), null, e));
        }
    }

    private Block block(long number, String hash, String prevHash) {
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        when(block.getHeader().getHeaderBody().getBlockNumber()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getSlot()).thenReturn(number);
        when(block.getHeader().getHeaderBody().getBlockHash()).thenReturn(hash);
        when(block.getHeader().getHeaderBody().getPrevHash()).thenReturn(prevHash);
        when(block.getHeader().getHeaderBody().getIssuerVkey()).thenReturn(ISSUER_VKEY);
        return block;
    }

    private Cursor cursor(long number, String hash) {
        return Cursor.builder().slot(number).block(number).blockHash(hash).prevBlockHash("prev-" + hash).era(Era.Conway).build();
    }
}
